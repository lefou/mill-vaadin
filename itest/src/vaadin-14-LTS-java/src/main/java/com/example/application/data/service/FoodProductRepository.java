package com.example.application.data.service;

import com.example.application.data.entity.FoodProduct;

import org.springframework.data.jpa.repository.JpaRepository;
import javax.persistence.Lob;

public interface FoodProductRepository extends JpaRepository<FoodProduct, Integer> {

}