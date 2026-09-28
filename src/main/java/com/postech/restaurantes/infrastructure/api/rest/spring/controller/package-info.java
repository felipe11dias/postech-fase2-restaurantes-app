/**
 * Controllers REST (@RestController, sufixo RestController): camada fina que valida a sintaxe,
 * converte o request em DTO do caso de uso, delega ao controller de adaptação e monta a resposta
 * com o assembler. Nenhuma regra de negócio e nenhum tratamento de exceção aqui.
 */
package com.postech.restaurantes.infrastructure.api.rest.spring.controller;
