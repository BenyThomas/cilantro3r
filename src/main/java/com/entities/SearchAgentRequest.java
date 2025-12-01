/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import lombok.*;

import javax.validation.constraints.NotBlank;

/**
 *
 * @author arthur.ndossi
 */
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@NoArgsConstructor
@ToString
public class SearchAgentRequest {
    @NotBlank(message = "Terminal no is required")
    private String terminal;

}
