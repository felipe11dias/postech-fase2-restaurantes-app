/**
 * ADAPTADORES DE INTERFACE. Depende de application e domain; nunca de infrastructure. Tradução e orquestração, sem regra de negócio.
 *
 * <p>O controller é o maestro, o gateway o tradutor e o presenter prepara a saída (Aulas 02 e 05;
 * Martin, caps. 22 e 23). O gateway consome origens de dados (datasource) e serviços externos
 * (service) sempre por interface (Aula 06). Ver docs/arquitetura/03-adaptadores.md.
 */
package com.postech.restaurantes.adapter;
