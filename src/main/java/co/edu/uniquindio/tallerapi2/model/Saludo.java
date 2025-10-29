package co.edu.uniquindio.tallerapi2.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saludos")
public class Saludo {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mensaje", nullable = false, length = 500)
    private String mensaje;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Constructores
    public Saludo() {
        this.fecha = LocalDateTime.now();
    }

    public Saludo(String mensaje, Usuario usuario) {
        this();
        this.mensaje = mensaje;
        this.usuario = usuario;
    }

    // Getters y setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    // toString para debugging
    @Override
    public String toString() {
        return "Saludo{" +
                "id=" + id +
                ", mensaje='" + mensaje + '\'' +
                ", fecha=" + fecha +
                ", usuario=" + (usuario != null ? usuario.getNombre() : "null") +
                '}';
    }

    // equals y hashCode basados en ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Saludo saludo = (Saludo) o;
        return id != null && id.equals(saludo.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}