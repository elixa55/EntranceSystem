package matchfinger;
import java.util.*;

class ImmutableMatcher {
    static final ImmutableMatcher empty = new ImmutableMatcher();
    final ImmutableTemplate template;
    final HashMap<Integer, List<IndexedEdge>> edgeHash;
    private ImmutableMatcher() {
        template = ImmutableTemplate.empty;
        edgeHash = new HashMap<>();
    }
    ImmutableMatcher(ImmutableTemplate template, HashMap<Integer, List<IndexedEdge>> edgeHash) {
        this.template = template;
        this.edgeHash = edgeHash;
    }
}
