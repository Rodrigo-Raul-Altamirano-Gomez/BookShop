package me.Business.opinion;

import me.Business.exceptions.NegocioException;

public interface VotoUtilBO {
    boolean cambiarVoto(int idActor, int idResena, boolean marcar) throws NegocioException;
}
