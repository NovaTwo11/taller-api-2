package co.edu.uniquindio.tallerapi2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Respuesta de un perfil de usuario")
public class PerfilResponse {

    private UUID userId;
    private String apodo;
    private String biografia;
    private String paginaPersonal;
    private String direccion;
    private String organizacion;
    private String paisResidencia;
    private Boolean informacionPublica;
    private Map<String, String> redesSociales;

    // Constructor para el stub
    public PerfilResponse(UUID userId, PerfilRequest request) {
        this.userId = userId;
        this.apodo = request.getApodo();
        this.biografia = request.getBiografia();
        this.paginaPersonal = request.getPaginaPersonal();
        this.direccion = request.getDireccion();
        this.organizacion = request.getOrganizacion();
        this.paisResidencia = request.getPaisResidencia();
        this.informacionPublica = request.getInformacionPublica();
        this.redesSociales = request.getRedesSociales();
    }

    // Getters y Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getApodo() { return apodo; }
    public void setApodo(String apodo) { this.apodo = apodo; }
    public String getBiografia() { return biografia; }
    public void setBiografia(String biografia) { this.biografia = biografia; }
    public String getPaginaPersonal() { return paginaPersonal; }
    public void setPaginaPersonal(String paginaPersonal) { this.paginaPersonal = paginaPersonal; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getOrganizacion() { return organizacion; }
    public void setOrganizacion(String organizacion) { this.organizacion = organizacion; }
    public String getPaisResidencia() { return paisResidencia; }
    public void setPaisResidencia(String paisResidencia) { this.paisResidencia = paisResidencia; }
    public Boolean getInformacionPublica() { return informacionPublica; }
    public void setInformacionPublica(Boolean informacionPublica) { this.informacionPublica = informacionPublica; }
    public Map<String, String> getRedesSociales() { return redesSociales; }
    public void setRedesSociales(Map<String, String> redesSociales) { this.redesSociales = redesSociales; }
}