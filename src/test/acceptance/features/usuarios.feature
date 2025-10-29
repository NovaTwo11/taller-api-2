Feature: Gestión de usuarios
  Como consumidor del API
  Quiero crear y consultar usuarios
  Para asegurar el correcto funcionamiento del microservicio

  Background:
    Given la API base está configurada

  Scenario: Crear un usuario exitosamente
    When creo un usuario con nombre aleatorio y email aleatorio válido
    Then la respuesta debe tener status 201
    And el cuerpo debe incluir el id del usuario creado

  Scenario: Obtener un usuario por ID existente
    Given existe un usuario previamente creado
    When consulto el usuario por su ID
    Then la respuesta debe tener status 200
    And el cuerpo debe incluir el email y nombre del usuario