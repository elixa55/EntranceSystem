package main;

enum MinutiaType {
    ENDING("ending"), BIFURCATION("bifurcation");
    final String typeName;
    MinutiaType(String typeName) {
        this.typeName = typeName;
    }
}

class Minutia {
    final Cell position;
    final double direction;
    final MinutiaType type;
    Minutia(Cell position, double direction, MinutiaType type) {
        this.position = position;
        this.direction = direction;
        this.type = type;
    }
    Minutia(MinutiaData minutiaName) {
        position = new Cell(minutiaName.x, minutiaName.y);
        direction = minutiaName.direction;
        type = MinutiaType.BIFURCATION.typeName.equals(minutiaName.type) ? MinutiaType.BIFURCATION : MinutiaType.ENDING;
    }
    @Override public String toString() {
        return String.format("%s @ %s angle %f", type.toString(), position.toString(), direction);
    }
}
