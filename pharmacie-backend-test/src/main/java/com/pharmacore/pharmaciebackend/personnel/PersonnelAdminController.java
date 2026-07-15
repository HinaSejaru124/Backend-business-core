package com.pharmacore.pharmaciebackend.personnel;

import com.pharmacore.pharmaciebackend.auth.PharmacoreSession;
import com.pharmacore.pharmaciebackend.personnel.PersonnelDtos.CreerPersonnelRequest;
import com.pharmacore.pharmaciebackend.personnel.PersonnelDtos.PersonnelResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Gestion du personnel — réservée au titulaire (brique 3, côté PharmaCore). Créer un membre du
 * personnel résout une vraie identité Business Core une seule fois ({@link PersonnelService#creer}) ;
 * ensuite, ce membre se connecte directement à PharmaCore (jamais au Kernel), cf. {@code AuthController}.
 */
@RestController
@RequestMapping("/api/admin/personnel")
public class PersonnelAdminController {

    private final PersonnelService service;
    private final PharmacoreSession session;

    public PersonnelAdminController(PersonnelService service, PharmacoreSession session) {
        this.service = service;
        this.session = session;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonnelResponse creer(@Valid @RequestBody CreerPersonnelRequest req) {
        session.exigerRole(PharmacoreSession.Role.TITULAIRE);
        return service.creer(req);
    }

    @GetMapping
    public List<PersonnelResponse> lister() {
        session.exigerRole(PharmacoreSession.Role.TITULAIRE);
        return service.lister();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactiver(@PathVariable UUID id) {
        session.exigerRole(PharmacoreSession.Role.TITULAIRE);
        service.desactiver(id);
    }
}
