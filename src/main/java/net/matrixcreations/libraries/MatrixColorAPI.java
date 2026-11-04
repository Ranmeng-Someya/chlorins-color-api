package net.matrixcreations.libraries;

import com.chlorins.colorapi.ChlorinsColors;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Drop-in replacement for the original MatrixColorAPI entry point.
 *
 * <p>The old library exposed exactly these two static methods; keeping them means existing code
 * compiles and behaves the same after swapping the jar for Chlorins Color API. The colour syntax is
 * a superset: {@code &} codes, {@code &#RRGGBB}, {@code <SOLID>} and {@code <GRADIENT>} tags are all
 * still understood, and the new {@code %#RRGGBB}, {@code @#RRGGBB} and
 * {@code %#RRGGBB-#RRGGBB} gradient markers are added on top.</p>
 *
 * <p>Output is a legacy {@code §} string, as before. Note that RGB colours are rendered as
 * {@code §x§R§R§G§G§B§B}, which vanilla clients do not understand - use
 * {@link com.chlorins.colorapi.ChlorinsColorApi#toText} if you are on the client.</p>
 */
public final class MatrixColorAPI {

    private MatrixColorAPI() {
    }

    public static String process(String text) {
        return ChlorinsColors.toLegacy(text);
    }

    public static List<String> process(List<String> texts) {
        return texts.stream().map(MatrixColorAPI::process).collect(Collectors.toList());
    }
}
