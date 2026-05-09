public class Test {
    public static void main(String[] args) {
        double[] starts = {0.5, 0.5, 0.5};
        double[] ends = {2.8, 0.5, 0.5};
        double distance = 2.3;

        int fullBlocks = (int) distance;
        float remainder = (float) (distance - fullBlocks);
        
        java.util.List<double[]> placedBlocks = new java.util.ArrayList<>();
        placedBlocks.add(new double[]{1.5, 0.5, 0.5}); // (1, 0, 0)
        placedBlocks.add(new double[]{2.5, 1.5, 0.5}); // (2, 1, 0) - adj block

        java.util.Map<Integer, java.util.List<Integer>> segmentMap = new java.util.HashMap<>();
        
        for (int i = 0; i <= fullBlocks; i++) {
            if (i == fullBlocks && remainder <= 0.001f) break;
            double segLength = (i == fullBlocks) ? remainder : 1.0;
            double[] segCenter = {
                starts[0] + (ends[0] - starts[0]) / distance * (i + segLength / 2.0),
                starts[1],
                starts[2]
            };
            
            int closest = -1;
            double minDist = Double.MAX_VALUE;
            for (int j = 0; j < placedBlocks.size(); j++) {
                double[] p = placedBlocks.get(j);
                double dist = Math.sqrt(Math.pow(p[0] - segCenter[0], 2) + Math.pow(p[1] - segCenter[1], 2));
                if (dist < minDist) {
                    minDist = dist;
                    closest = j;
                }
            }
            
            System.out.println("Segment " + i + " center " + segCenter[0] + " assigned to block " + closest);
        }
    }
}
