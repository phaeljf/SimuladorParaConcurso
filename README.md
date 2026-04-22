# 📚 Gerador de Simulados para Concursos

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![Render](https://img.shields.io/badge/Deploy-Render-46E3B7?style=for-the-badge&logo=render&logoColor=black)](https://render.com/)

---

## Sobre o Projeto

Aplicação desenvolvida em **Java + Spring Boot** com o objetivo de permitir a criação e realização de **simulados personalizados**.

O sistema possibilita que professores criem provas e compartilhem com seus alunos, assim como qualquer interessado pode realizar simulados personalizados com correção automática.

### Objetivos

- Facilitar a criação de simulados personalizados  
- Permitir prática direcionada por área de conhecimento  
- Automatizar correção de provas  
- Gerar simulados em PDF  

---

## Acesse o sistema

A aplicação está disponível online:

https://simuladorparaconcurso.onrender.com/

**Observação:**  
O sistema está hospedado no Render (plano gratuito), podendo entrar em modo de inatividade.  
Caso isso aconteça, aguarde alguns segundos até o servidor ser inicializado.

Aplicação em **Java + Spring Boot** para criação e realização de simulados de concursos, com área para professores, áreas de questões e provas.  
O objetivo é permitir a criação de provas personalizadas, para que qualquer aluno possa realizar simulados.  Professores cadastrados (via contato prévio) podem gerar suas próprias provas e repassar aos seus alunos para eles pesquisarem e realizarem as provas..

## Como Usar

### Professor
**OBS - Para acessar entrar em contato através do e-mai (suportesimuladordeconcurso@gmail.com) e solicitar um login**

- Criar questões 
- Montar provas  
- Definir tempo e quantidade por área  
- Compartilhar simulados  

### Aluno

- Criar seu próprio simulado
- Pesquisar simulados disponíveis  

---


## Tecnologias Utilizadas
- [Java 21](https://openjdk.org/projects/jdk/21/)
- [Spring Boot 3.5.4](https://spring.io/projects/spring-boot)
- [Spring Security (BCrypt)](https://spring.io/projects/spring-security)
- [Lombok](https://projectlombok.org/)
- [Maven](https://maven.apache.org/)
- [PostgreSQL](https://www.postgresql.org/)
- [Thymeleaf](https://www.thymeleaf.org/)
- Deploy no [Render](https://render.com/) e [Neon](https://neon.tech/)

---

## Funcionalidades

- Autenticação de professores (com senha criptografada - BCrypt)  
- CRUD completo de questões  
- Organização por áreas de conhecimento  
- Criação de provas personalizadas pelo professor
- Criação de provas públicas ou privadas
- Criação de link da prova privada
- Personalização de cada Simulado
- Correção automática  
- Download do simulado criado em PDF  

---

## Estrutura do Projeto
- `dominio/` - Entidades principais (Professor, Prova, Questão, Área de Conhecimento, etc.)
- `modelo/` - Objetos auxiliares (sessão do professor, exibição de questão)
- `repositorio/` - Interfaces de persistência com Spring Data JPA
- `servico/` - Serviços da aplicação (geração de PDF, simulado)
- `util/` - Classes utilitárias e helpers
- `web/` - Controllers para rotas públicas e administrativas
- `web/dto/` - Objetos de transferência de dados (DTOs)


---

## ⚙️ Tecnologias Utilizadas

- Java 21  
- Spring Boot 3.5.4  
- Spring Security (BCrypt)  
- Lombok  
- Maven  
- PostgreSQL  
- Thymeleaf  
- Deploy: Render + Neon  

---

## ⚙️ Como Executar Localmente

### Pré-requisitos

- Java 21  
- Maven  
- PostgreSQL  

---

## Autor

Desenvolvido por **Raphael de Lemos Pires**

- GitHub: https://github.com/phaeljf  
- LinkedIn: https://www.linkedin.com/in/raphaellpires/

---

## Licença

Este projeto foi desenvolvido para fins de estudo e prática.  
Sinta-se à vontade para utilizar como base para seus próprios projetos.
Licença MIT.