/**
 * Interfaces dos serviços externos que os gateways consomem (IMailSender, ITokenEncoder) — o
 * par de adapter/datasource para o que não é origem de dados. Implementadas em infrastructure.
 *
 * <p>É a regra das aulas (Aulas 05 e 06): o caso de uso consome o gateway por uma interface, e o
 * gateway consome o serviço externo por outra. O gateway traduz (monta o e-mail, os claims do
 * token); a implementação na infraestrutura só transporta. Ver docs/arquitetura/03-adaptadores.md.
 */
package com.postech.restaurantes.adapter.service;
