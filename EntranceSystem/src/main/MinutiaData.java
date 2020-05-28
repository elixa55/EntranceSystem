package main;

class MinutiaData {
    int x;
    int y;
    double direction;
    String type;
    MinutiaData(Minutia minutia) {
        x = minutia.position.x;
        y = minutia.position.y;
        direction = minutia.direction;
        type = minutia.type.typeName;
    }
}