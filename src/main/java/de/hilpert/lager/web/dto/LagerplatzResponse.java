package de.hilpert.lager.web.dto;

import de.hilpert.lager.domain.Lagerplatz;

public record LagerplatzResponse(String code, int gang, int regal, int ebene, int kapazitaet,
                                 Integer laengeMm, Integer breiteMm, Integer hoeheMm, Integer maxGewichtKg,
                                 boolean gesperrt, String sperrgrund) {

    public static LagerplatzResponse von(Lagerplatz platz) {
        return new LagerplatzResponse(platz.getCode(), platz.getGang(), platz.getRegal(), platz.getEbene(),
                platz.getKapazitaet(), platz.getLaengeMm(), platz.getBreiteMm(), platz.getHoeheMm(),
                platz.getMaxGewichtKg(), platz.isGesperrt(), platz.getSperrgrund());
    }
}
