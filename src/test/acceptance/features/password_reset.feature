Feature: Recuperación de contraseña
  Como usuario
  Quiero poder solicitar y realizar el restablecimiento de contraseña
  Para recuperar el acceso a mi cuenta

  Background:
    Given la API base está configurada

  Scenario: Solicitar reset de contraseña para un email existente
    Given existe un usuario con email aleatorio
    When solicito recuperación de contraseña para ese email
    Then la respuesta debe tener status 200

  Scenario: Resetear contraseña con token válido
    Given existe un token de recuperación válido para el usuario
    When envío la nueva contraseña con el token
    Then la respuesta debe tener status 200
    And el mensaje indica "Contraseña actualizada correctamente" o equivalente