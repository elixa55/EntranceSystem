package matchfinger;

class IndexedEdge extends EdgeShape {
    final int reference;
    final int neighbor;
    IndexedEdge(Minutia[] minutiae, int reference, int neighbor) {
        super(minutiae[reference], minutiae[neighbor]);
        this.reference = reference;
        this.neighbor = neighbor;
    }
    
    
}
