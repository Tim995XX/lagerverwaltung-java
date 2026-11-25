package de.hilpert.lager.web;

import de.hilpert.lager.domain.UngueltigerStatusException;
import de.hilpert.lager.fehler.KonfliktException;
import de.hilpert.lager.fehler.NichtGefundenException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Übersetzt Exceptions zentral in HTTP-Antworten im Format "Problem Details" (RFC 9457),
 * damit die Controller frei von try/catch bleiben.
 */
@RestControllerAdvice
public class FehlerBehandlung {

    /** SQLSTATE für eine verletzte Unique-Regel (unique_violation). */
    private static final String UNIQUE_VERLETZT = "23505";

    @ExceptionHandler(NichtGefundenException.class)
    ProblemDetail nichtGefunden(NichtGefundenException e) {
        return problem(HttpStatus.NOT_FOUND, "Nicht gefunden", e.getMessage());
    }

    @ExceptionHandler({KonfliktException.class, UngueltigerStatusException.class})
    ProblemDetail konflikt(RuntimeException e) {
        return problem(HttpStatus.CONFLICT, "Konflikt", e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail gleichzeitigeAenderung(ObjectOptimisticLockingFailureException e) {
        return problem(HttpStatus.CONFLICT, "Konflikt",
                "Der Datensatz wurde gleichzeitig geändert, bitte erneut versuchen");
    }

    /**
     * Doppelter Wert, z. B. weil zwei Wareneingänge gleichzeitig denselben leeren Platz belegen.
     * Andere Verstöße (z. B. negative Menge) sind Programmfehler und bleiben ein 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail doppelterWert(DataIntegrityViolationException e) {
        if (e.getMostSpecificCause() instanceof SQLException sql
                && UNIQUE_VERLETZT.equals(sql.getSQLState())) {
            return problem(HttpStatus.CONFLICT, "Konflikt",
                    "Der Wert wurde gleichzeitig vergeben, bitte erneut versuchen");
        }
        throw e;
    }

    /** Bean Validation (@Valid) fehlgeschlagen: alle Feldfehler gesammelt zurückgeben. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail ungueltigeEingabe(MethodArgumentNotValidException e) {
        Map<String, String> felder = new TreeMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fehler -> felder.putIfAbsent(fehler.getField(), fehler.getDefaultMessage()));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Ungültige Eingabe", "Bitte Felder prüfen");
        problem.setProperty("felder", felder);
        return problem;
    }

    /** Kaputtes JSON oder unbekannter Enum-Wert, z. B. "prioritaet": "DRINGEND". */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail nichtLesbar(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "Ungültige Eingabe",
                "Anfrage nicht lesbar (ungültiges JSON oder unbekannter Wert)");
    }

    /** Falscher Typ in Pfad oder Query, z. B. /api/transportauftraege/abc oder ?status=FALSCH. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail falscherTyp(MethodArgumentTypeMismatchException e) {
        return problem(HttpStatus.BAD_REQUEST, "Ungültige Eingabe",
                "Ungültiger Wert für Parameter '" + e.getName() + "'");
    }

    private static ProblemDetail problem(HttpStatus status, String titel, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(titel);
        return problem;
    }
}
