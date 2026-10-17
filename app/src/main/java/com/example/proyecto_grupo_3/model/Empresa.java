package com.example.proyecto_grupo_3.model;

import com.example.proyecto_grupo_3.R;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Empresa implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("descripcion")
    private String descripcion;

    @SerializedName("ruc")
    private String ruc;

    @SerializedName("num_trabajadores")
    private int numTrabajadores;

    @SerializedName("activo")
    private boolean activo;

    private int iconResId;
    private boolean isSelected;

    public Empresa() {
    }

    public Empresa(int id, String nombre, String descripcion, String ruc, int numTrabajadores, boolean activo, int iconResId, boolean isSelected) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.ruc = ruc;
        this.numTrabajadores = numTrabajadores;
        this.activo = activo;
        this.iconResId = iconResId;
        this.isSelected = isSelected;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            return getDefaultDescription();
        }
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public int getNumTrabajadores() {
        return numTrabajadores;
    }

    public void setNumTrabajadores(int numTrabajadores) {
        this.numTrabajadores = numTrabajadores;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public int getIconResId() {
        if (iconResId == 0) {
            return getDefaultIcon();
        }
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    private int getDefaultIcon() {
        if (nombre != null) {
            String nameLower = nombre.toLowerCase();
            if (nameLower.contains("epsel") || nameLower.contains("saneamiento") || nameLower.contains("agua")) {
                return R.drawable.ic_drop_water;
            } else if (nameLower.contains("servicio") || nameLower.contains("mantenimiento") || nameLower.contains("2")) {
                return R.drawable.ic_wrench_services;
            } else if (nameLower.contains("tecno") || nameLower.contains("solucion") || nameLower.contains("3")) {
                return R.drawable.ic_tech_chip;
            } else if (nameLower.contains("ambient") || nameLower.contains("eco") || nameLower.contains("4")) {
                return R.drawable.ic_leaf_eco;
            } else if (nameLower.contains("industria") || nameLower.contains("comercio") || nameLower.contains("5")) {
                return R.drawable.ic_factory_store;
            }
        }
        return R.drawable.ic_drop_water;
    }

    private String getDefaultDescription() {
        if (nombre != null) {
            String nameLower = nombre.toLowerCase();
            if (nameLower.contains("epsel")) {
                return "Empresa Prestadora de Servicios de Saneamiento.";
            } else if (nameLower.contains("2") || nameLower.contains("general")) {
                return "Servicios Generales.";
            } else if (nameLower.contains("3") || nameLower.contains("tecno")) {
                return "Soluciones Tecnológicas.";
            } else if (nameLower.contains("4") || nameLower.contains("ambient")) {
                return "Servicios Ambientales.";
            } else if (nameLower.contains("5") || nameLower.contains("comercio")) {
                return "Industria y Comercio.";
            }
        }
        return "Servicios Especializados.";
    }
}
