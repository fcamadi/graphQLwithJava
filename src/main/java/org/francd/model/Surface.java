package org.francd.model;

public record Surface(Double amount, Unit unit) {

    @Override
    public String toString() {
        return amount + " " + unit.stringValue;
    }

    public enum Unit {
        KM2("km²"),
        M2("m²");

        private final String stringValue;

        Unit(String stringValue) {
            this.stringValue = stringValue;
        }


        public static Unit of(String unitStr) {
            return switch (unitStr.toLowerCase()) {
                case "m2", "m²" -> M2;
                default -> KM2;
            };
        }
    }
}
