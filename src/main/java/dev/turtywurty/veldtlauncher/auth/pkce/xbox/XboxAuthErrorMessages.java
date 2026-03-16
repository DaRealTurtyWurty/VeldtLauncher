package dev.turtywurty.veldtlauncher.auth.pkce.xbox;

public final class XboxAuthErrorMessages {
    private XboxAuthErrorMessages() {
    }

    public static String describe(Long xErr, String redirect) {
        if (xErr == null)
            return null;

        return switch ((int) (long) xErr) {
            case 0x8015DC03 -> resolveWithXbox(
                    "This Xbox account is enforcement banned.",
                    redirect
            );
            case 0x8015DC05 -> resolveWithXbox(
                    "This Xbox account is blocked by parental restrictions.",
                    redirect
            );
            case 0x8015DC09 -> resolveWithXbox(
                    "This Microsoft account does not have a usable Xbox profile yet. Complete Xbox account creation, accept any prompts, and choose a gamertag before trying again.",
                    redirect
            );
            case 0x8015DC0A -> resolveWithXbox(
                    "This account still needs to accept Xbox terms of use.",
                    redirect
            );
            case 0x8015DC0B -> resolveWithXbox(
                    "This account is in a country or region that is not authorized for Xbox services.",
                    redirect
            );
            case 0x8015DC0C -> resolveWithXbox(
                    "This account still needs age verification for Xbox services.",
                    redirect
            );
            case 0x8015DC0D -> resolveWithXbox(
                    "This account is currently blocked by Xbox account curfew settings.",
                    redirect
            );
            case 0x8015DC0E -> resolveWithXbox(
                    "This child account is not part of a Microsoft family for Xbox access.",
                    redirect
            );
            case 0x8015DC0F -> resolveWithXbox(
                    "This account needs additional Xbox migration or conversion steps before sign-in can continue.",
                    redirect
            );
            case 0x8015DC10 -> resolveWithXbox(
                    "This account requires Xbox account maintenance before sign-in can continue.",
                    redirect
            );
            case 0x8015DC12 -> "The requested Xbox sandbox is not authorized for this account or application.";
            case 0x8015DC13 -> resolveWithXbox(
                    "This account needs to finish a gamertag change flow before sign-in can continue.",
                    redirect
            );
            case 0x8015DC1F -> "The Xbox service token used for this request has expired.";
            case 0x8015DC22 -> "The Xbox user token used for this request has expired.";
            case 0x8015DC26 -> "The Xbox user token used for this request is invalid.";
            case 0x8015DC27 -> "The Xbox service token used for this request is invalid.";
            case 0x8015DC31, 0x8015DC32 -> "Xbox authentication services are currently unavailable.";
            default -> null;
        };
    }

    private static String resolveWithXbox(String issue, String redirect) {
        String destination = isBlank(redirect) ? "https://xbox.com" : redirect;
        return issue + " Open " + destination + ", complete the required Xbox setup, and then try again.";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
