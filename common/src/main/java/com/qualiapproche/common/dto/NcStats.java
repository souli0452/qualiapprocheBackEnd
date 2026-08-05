package com.qualiapproche.common.dto;



import com.qualiapproche.common.enumeration.Status;

public interface NcStats {
    Status getStatus();
    Integer getCount();
}
