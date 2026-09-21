#!/bin/sh
# Compare la carte des menus d'ia-service au menu réel du front.
#
# La carte vit ici, le menu vit là-bas : rien dans la compilation ne les relie, et un menu ajouté
# au front sans être reporté ici fait mentir l'assistant sans que rien ne le signale. Ce script
# est le rattrapage manuel, à passer quand le menu bouge.
#
# Usage : sh outils/verifier-carte-des-menus.sh [chemin-du-front]

FRONT="${1:-../FrontQualiApproche}"
MENU="$FRONT/src/app/core/layout/component/app.menu.ts"
CARTE="ia-service/src/main/resources/carte-des-menus.txt"

[ -f "$MENU" ] || { echo "Front introuvable : $MENU"; exit 2; }

grep -oE "routerLink: *\[ *'[^']+'" "$MENU" | sed -E "s/.*'([^']+)'/\1/" | sort -u > /tmp/routes-front.txt
grep -oE '= /[^ ]*$|= /$' "$CARTE" | sed -E 's/^= //' | sort -u > /tmp/routes-carte.txt

echo "routes du front : $(wc -l < /tmp/routes-front.txt)"
echo "routes de la carte : $(wc -l < /tmp/routes-carte.txt)"
echo

MANQUE=$(comm -23 /tmp/routes-front.txt /tmp/routes-carte.txt)
TROP=$(comm -13 /tmp/routes-front.txt /tmp/routes-carte.txt)

[ -n "$MANQUE" ] && { echo "DANS LE FRONT, ABSENT DE LA CARTE :"; echo "$MANQUE"; }
[ -n "$TROP" ]   && { echo "DANS LA CARTE, ABSENT DU FRONT :";   echo "$TROP"; }

if [ -z "$MANQUE" ] && [ -z "$TROP" ]; then
    echo "La carte correspond au menu du front."
else
    echo
    echo "Reporter les écarts dans $CARTE."
    exit 1
fi
