package g2.be;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashCracker {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String[] hashes = {
            "$2a$10$0DukDnxdIII8x0N2a08qXOM2z6H3vS9.CT6OrsA4Xv4VgNUofnl5.",
            "$2a$10$J2jgVfQwv76n2qW9ZSBRW.e7Fwh8wJcmbvbniAw1kRQMijPBDd7qC",
            "$2a$10$9ojJgfhf.S8CAD3hd2/PSe2hICYE/yUChhoJuSS1jXpr5/B7ng25O",
            "$2a$10$3auGdlabngB1TMdt5FCAruZ1beAaGVZfaVfk.n/CuL5cHdbLr2uZC",
            "$2a$10$FIYQQpptBn5LcRGLSAKNoOrC234U2k4MlgJlyLiqh4hIX.jJSpzta",
            "$2a$10$a8J3t3EKFyjITT6ZjL9fneIjEupAXXOuol.t.lDT0jtDQoh96G6..",
            "$2a$10$9Ieu7RjKI/ueMfeJtgpWi.3OBR/YWMWLHvqpJubB8ygZC38tCw6k.",
            "$2a$10$wvkEw0zDI4qoztXPZBVX6utwWLVfj1pW95KP3ThGlSokHvLi1sYjq",
            "$2a$10$oeZ.NaDtAxITQA.H.thiIuVqSoBaj7mraoBDMojnNEnfQrKRFC1LG",
            "$2a$10$bC3SP043oCSMLSPUbxwqDevr8uTE0vicSgipIlTMgkOvEr/sjTdtC",
            "$2a$10$WdZQiFNwh4iFYiN74obwgeQRF6n1U.efXAYuC49dKQh6WcIE29hFu",
            "$2a$10$OPzr.l3gDmN7Vx5x2FUDeO1eSqzg5PGoj6cZxU.MVtUNwX//pyd4G",
            "$2a$10$Huzesn9mb0Eb728Rj4boz.rtb.uJXYTUaNuXWOx2HiZg54gS9n69W",
            "$2a$10$CpVT9QXrP3MHXhMka0.WtuDH9TkbvlEEHcML314CERIz1qZZR.IZm",
            "$2a$10$uhrt87jeJQluqdJYAFa/oOH.kP6a9kXUvJcpDdfODfqJO/3mmCUOe",
            "$2a$10$HW4GrXWolnFXR6MLiYbDeuSUqHtHIs4dr61hO0q6cy02U6MrcRM0m",
            "$2a$10$D.NiRub7GwRc/a2Mb/fcl.QiqBDYgr.FXOOqoLBVjSUbINZ8oItUe",
            "$2a$10$w44AmNNxTNEPS5HMS2f/Jeqbg1.joqMdGSo75Qamv.EPVQmkVtaD2",
            "$2a$10$8wichBhpYboruiSALPtn9.rLeZPMo0zKmvJgShpSTgZJeUQCFx/Ni",
            "$2a$10$V8Gd/b9kjBBndWgow4vqW.cTLT4rKq/8Rg4kveErQDpcX.rUDBFpa",
            "$2a$10$tuDsrmj/Q1xstKttxzXqA.v6kXDBLHcsJt642Vv2W4sf/n4yT/yty",
            "$2a$10$MgsvZwukNh63E8UK8BeZweTXTWld7VU0UEntGaLvVLuC/xa6CB/BK",
            "$2a$10$Ca8TzT/v3HHdNxeh4FO9Ve53S2tAey9RXcAHo0id4EnqNzwmBZ75O",
            "$2a$10$.UXOQ6pstldHlu8dpm/EPux6pfUh/o1Bc0WT0sbv1.xhygrIfnt2G",
            "$2a$10$Cmd1txcGFR1BwB98oZ8p/uWL2dFwOI3FpMQKztxvzLevKqht9huWK",
            "$2a$10$5Xs70US.8Ukfg6xet05h9OuhUSsjs0hr03srrv4nX1cbcqvNK1E8.",
            "$2a$10$7sBczYch4rtocCblF0OzOOLZtJInLd.6e4lLG3OMMe9xSJ.yxgHpO",
            "$2a$10$DmJkMBpwTgoBVD25gr8wK..64YrDdhNOLTZFBWMS3rjhVodNGSW/6",
            "$2a$10$asYXGJvCW5Tp/LOLwlbQQuoxcctMjh1QFFKvCbdBtxtBK86iOCDZm",
            "$2a$10$nhcblD5tZOwonB1/CSMCkuQ0ohP8Ljk9QfZ59Q4kOIbC8DmuWkaqu",
            "$2a$10$eFGzJvN0UTFnoiOfFT6H5em4DnopS4hqrIicUNgc8mQX1vS8Z5EzO",
            "$2a$10$7EEBk3XeZxeFkAzkzaXx9etf1v8ywHd1GeMk9p3IWNMMAo0fhOkbi",
            "$2a$10$zHmtdHLFzaiqhwOe5ze4dOX6yDKZzq6KDRDDS9kbdLMGnMZRmxc6u",
            "$2a$10$dMs6MfrKtA/BnwKPR.iH9.LX1qy1HovWFCHkx3bsvl7hpOynVrRoS",
            "$2a$10$nMIRN.hedepNw.IXGg5GKefFyOmZ/8CBQ.EBby12EIqfZzw1LABwq",
            "$2a$10$wNkK5cFXuZKOizXEzehrjen3njBOp8FNxhaQ6is7M7.2xP7/18kQu",
            "$2a$10$k8pCQ55XeIdS9oj0Pcu8G.LA4Q90Dtnz2IgiLw26YVFz7jHuAFXgu",
            "$2a$10$SclNR5gq3pM2l0x1UWctYOoy2uMsmO3KrvrUSUxsBN0TW8jfcMwm6",
            "$2a$10$N9jfWP5PUT5A9ryEiqtoTerKKCUshIDzxqbk2w3oJdUy0Twxz81UK",
            "$2a$10$a.TLgIqF0DooLBA3yaSTYuopxY41oo/dvGNuBpGvrQ/sjDwW11xXO",
            "$2a$10$l3o0sP.sadTv/fnL0NA4AepAqP0QzL81k/f.Ngfb0qgDjAvh1DDoy",
            "$2a$10$NP9F/FXB.oByE8dfoLb5BOAUXRQBl/kJ7QWfxLhqW3erSGT4uPRzu",
            "$2a$10$JVwLoKxnO5bJMnmMTKZ0cugmGIPIvCKSumhdIUS3sZQ9.kuYYF6.e"
        };
        String[] passwords = {
            "123456", "admin123", "password", "DA123456", "111111", "000000", "0123456789", 
            "123456aA@", "admin", "12345678", "letan123", "bacsi123", "yta123", "patient123"
        };

        for (String h : hashes) {
            String found = "UNKNOWN";
            for (String p : passwords) {
                if (encoder.matches(p, h)) {
                    found = p;
                    break;
                }
            }
            System.out.println(h + " : " + found);
        }
    }
}
