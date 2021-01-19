package com.example.application.data.entity;

import javax.persistence.Entity;

import com.example.application.data.AbstractEntity;
import javax.persistence.Lob;

@Entity
public class FoodProduct extends AbstractEntity {

    @Lob
    private String image;
    private String name;
    private String eanCode;

    public String getImage() {
        return image;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEanCode() {
        return eanCode;
    }
    public void setEanCode(String eanCode) {
        this.eanCode = eanCode;
    }

}
