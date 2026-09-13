# QualiSira en local avec Docker

Une commande démarre toute la stack : `docker compose up -d`.

## Prérequis

1. **Docker** avec le plugin compose v2 (`docker compose version`).
2. **`.env`** : `cp .env.example .env`, puis renseigner au minimum :
   - `DB_PASSWORD` — mot de passe postgres local (libre, ex. `postgres`),
   - `KC_CLIENT_SECRET` — secret du client `quali-sira` (voir « Keycloak » ci-dessous),
   - `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` — identifiants du MinIO local (libres ;
     le mot de passe doit faire **8 caractères minimum**, ex. `minioadmin` / `minioadmin`),
   - `QUALISIRA_CLE_PUBLIQUE` — sans elle, referentiel-service ne démarre pas,
   - `IA_API_KEY` — sans elle, ia-service ne démarre pas (ou `ollama` si IA_BASE_URL
     pointe vers un Ollama local).
3. **Keycloak** : la stack utilise par défaut **celui qui tourne déjà sur l'hôte**
   (`http://localhost:8080`, realm `quali`) — les conteneurs l'atteignent via
   `host.docker.internal`. Rien à faire si ton Keycloak local est démarré.
   Aucune ligne /etc/hosts n'est nécessaire dans ce mode.
   
   Pour un Keycloak 100 % conteneurisé (machine neuve, sans Keycloak local) :
   `docker compose --profile keycloak-local up -d`, ajouter `127.0.0.1 keycloak` dans
   /etc/hosts, réaligner `KC_ISSUER_URI` / `KC_TOKEN_URI` / `KC_SERVER_URL` sur
   `http://keycloak:8080` dans l'ancre `x-env-spring` du docker-compose.yml, puis créer
   ou importer le realm (voir « Keycloak : le realm quali »).

## Démarrage

```sh
docker compose up -d            # build les 12 images (long au premier coup : builds maven)
docker compose up -d --build user-service   # reconstruire un seul service après un changement
docker compose logs -f user-service         # suivre les journaux d'un service
docker compose ps                           # état (healthy/unhealthy)
docker compose down                         # tout arrêter
docker compose down -v                      # tout arrêter ET effacer bases + fichiers (repartir de zéro)
```

Ordre de démarrage attendu : postgres → discovery → les 10 services métier → api-gateway.
Compter 2 à 4 minutes après la fin des builds pour que tout soit enregistré dans Eureka.

## URLs

| Quoi | URL |
|---|---|
| Passerelle (point d'entrée de l'API) | http://localhost:8088 |
| Eureka | http://localhost:8761 |
| Keycloak (celui de l'hôte) | http://localhost:8080/admin |
| Console MinIO | http://localhost:9001 (MINIO_ACCESS_KEY / MINIO_SECRET_KEY) |
| Mailhog (courriels interceptés) | http://localhost:8025 |
| Frontal (profil `front`) | http://localhost:4200 |

Les services métier ne publient aucun port : tout passe par la passerelle. Pour joindre
un service directement en debug : `docker compose exec user-service ...` ou publier un
port à la main via un fichier d'override.

## Keycloak : le realm `quali`

Par défaut, la stack utilise le Keycloak de l'hôte : le realm `quali`, le client
`quali-sira` et les utilisateurs y existent déjà. Vérifier seulement que le secret du
client est bien reporté dans `KC_CLIENT_SECRET` du `.env`.

La section ci-dessous ne concerne que le profil `keycloak-local` (Keycloak conteneurisé,
realm vierge). Aucun export du realm n'est versionné ; deux options :

- **Importer un export existant** : depuis une instance QualiSira qui tourne, exporter le
  realm (console → Realm settings → Action → Partial export, ou
  `/opt/keycloak/bin/kc.sh export --realm quali --file realm.json`), le déposer dans
  `docker/keycloak/import/quali-realm.json` puis redémarrer keycloak
  (`docker compose up -d --force-recreate keycloak`). Il est importé automatiquement
  (`--import-realm`).
- **Le créer à la main** (http://keycloak:8080/admin, admin/admin) :
  1. Créer le realm `quali`.
  2. Créer le client `quali-sira` : *Client authentication* activé (confidential),
     *Service accounts* activé, et recopier le secret généré (onglet Credentials) dans
     `KC_CLIENT_SECRET` du `.env`, puis `docker compose up -d` pour le propager.
  3. Recréer les rôles et les utilisateurs attendus par l'application.

## Frontal

Le frontal vit dans le dépôt voisin `../FrontQualiApproche` et est exclu par défaut :

```sh
docker compose --profile front up -d
```

Son URL d'API est figée dans le bundle au build : le Dockerfile du frontal accepte
l'argument `API_URL` (le compose passe `http://localhost:8088`) et remplace l'URL de
test dans les fichiers compilés. Sans cet argument, le build reste inchangé
(pointe vers l'environnement de test).

## Dépannage

- **Un service redémarre en boucle** : `docker compose logs <service>`. Causes typiques :
  base absente ou mot de passe erroné (DB_PASSWORD), `KC_CLIENT_SECRET` absent ou
  invalide, `QUALISIRA_CLE_PUBLIQUE` absente (referentiel), `IA_API_KEY` absente (ia).
  Le .env distingue les variables obligatoires des réglages par défaut.
- **Erreurs `iss` / 401 partout** : le jeton vient d'un autre Keycloak que celui visé par
  `KC_ISSUER_URI` (autre realm, autre URL), ou les trois URL KC_* ne portent pas la même
  base.
- **Les courriels ne partent pas** : c'est normal, Mailhog les capture — http://localhost:8025.
- **Les pièces jointes échouent** : vérifier que le bucket existe (console MinIO,
  http://localhost:9001) ; le conteneur `minio-init` le crée au démarrage
  (`docker compose logs minio-init`).
- **Repartir de zéro sur les données** : `docker compose down -v` (supprime les volumes
  postgres et minio ; init-db.sql recrée les bases au démarrage suivant).
