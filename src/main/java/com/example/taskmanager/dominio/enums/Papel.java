package com.example.taskmanager.dominio.enums;

/**
 * Papel do usuario dentro de um projeto especifico (nao e uma permissao global).
 * O dono do projeto sempre comeca como ADMIN; outros membros podem ser
 * promovidos a ADMIN para ajudar a administrar o projeto.
 *
 * Os valores ficam em ingles porque assim foram definidos no enunciado do
 * desafio (ADMIN / MEMBER) e sao usados como contrato da API.
 */
public enum Papel {
    ADMIN,
    MEMBER
}
