# Gerador de Simulado Online

Aplicação em **Java + Spring Boot** para criação e realização de simulados de concursos, com área para professores, áreas de questões e provas.  
O objetivo é permitir a criação de provas personalizadas, para que qualquer aluno possa realizar simulados.  Professores cadastrados (via contato prévio) podem gerar suas próprias provas e repassar aos seus alunos para eles pesquisarem e realizarem as provas..


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

## Estrutura do Projeto
- `dominio/` => Entidades principais (Professor, Prova, Questão, Área de Conhecimento, etc.)
- `modelo/` => Objetos auxiliares (sessão do professor, exibição de questão)
- `repositorio/` => Interfaces de persistência com Spring Data JPA
- `servico/` => Serviços da aplicação (geração de PDF, simulado)
- `util/` => Classes utilitárias e helpers
- `web/` => Controllers para rotas públicas e administrativas
- `web/dto/` => Objetos de transferência de dados (DTOs)


---

## Funcionalidades
- Login de professores (com senha criptografada em BCrypt)
- CRUD de Questões
- Geração de questões, provas e simulados
- Correção automática de simulados
- Exportação em PDF do simulado realizado
- Criação de simulado personalizado (quantidade de questões por área e tempo configurável)
