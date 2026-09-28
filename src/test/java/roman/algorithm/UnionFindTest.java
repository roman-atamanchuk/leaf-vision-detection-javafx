package roman.algorithm;

import org.junit.jupiter.api.Test;
import roman.UnionFind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UnionFindTest {

    @Test
    void unionsValuesIntoSameSet() {
        UnionFind unionFind = new UnionFind(6);

        unionFind.union(0, 1);
        unionFind.union(1, 2);
        unionFind.union(4, 5);

        assertEquals(unionFind.find(0), unionFind.find(2));
        assertEquals(unionFind.find(4), unionFind.find(5));
    }

    @Test
    void leavesUnconnectedValuesInDifferentSets() {
        UnionFind unionFind = new UnionFind(4);

        unionFind.union(0, 1);

        assertNotEquals(unionFind.find(0), unionFind.find(2));
        assertNotEquals(unionFind.find(1), unionFind.find(3));
    }

    @Test
    void repeatedUnionDoesNotBreakExistingSet() {
        UnionFind unionFind = new UnionFind(3);

        unionFind.union(0, 1);
        unionFind.union(1, 0);
        unionFind.union(0, 1);

        assertEquals(unionFind.find(0), unionFind.find(1));
    }

    @Test
    void unionWithSelfKeepsSameRoot() {
        UnionFind unionFind = new UnionFind(2);

        unionFind.union(1, 1);

        assertEquals(1, unionFind.find(1));
    }
}
