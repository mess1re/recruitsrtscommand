package me.mss1r.recruitsrtscommand.client.command;

import me.mss1r.recruitsrtscommand.common.command.CommandGroupSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitSnapshot;
import me.mss1r.recruitsrtscommand.common.command.CommandUnitType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SquadClusters {
    public static final int MIXED = -1;

    private static final double JOIN_DISTANCE = 28.0D;
    private static final double JOIN_DISTANCE_SQR = JOIN_DISTANCE * JOIN_DISTANCE;

    private SquadClusters() {
    }

    public static final class Cache {
        private long revision = Long.MIN_VALUE;
        private List<Cluster> clusters = List.of();

        public List<Cluster> update(long currentRevision, List<CommandGroupSnapshot> snapshots) {
            if (revision != currentRevision) {
                clusters = of(snapshots);
                revision = currentRevision;
            } else {
                clusters = clusters.stream().map(SquadClusters::interpolated).toList();
            }
            return clusters;
        }

        public void clear() {
            revision = Long.MIN_VALUE;
            clusters = List.of();
        }
    }

    public record Cluster(CommandGroupSnapshot snapshot, List<UUID> members,
                          double centerX, double centerZ, double reachNorth, int health,
                          int aggro, boolean shields, List<CommandUnitSnapshot> body) {
        public UUID selectionId() {
            return snapshot.selectionId();
        }

        public UUID ownerId() {
            return snapshot.ownerId();
        }

        public int strength() {
            return members.size();
        }

        public boolean whole() {
            return members.size() == snapshot.units().size();
        }
    }

    public static List<Cluster> of(List<CommandGroupSnapshot> snapshots) {
        List<Cluster> clusters = new ArrayList<>();
        for (CommandGroupSnapshot snapshot : snapshots) {
            clusters.addAll(of(snapshot));
        }
        return clusters;
    }

    public static List<Cluster> of(CommandGroupSnapshot snapshot) {
        List<CommandUnitSnapshot> units = snapshot.units();
        if (units == null || units.isEmpty()) return List.of();

        List<List<CommandUnitSnapshot>> bodies = new ArrayList<>();
        for (CommandUnitSnapshot unit : units) {
            List<CommandUnitSnapshot> home = null;
            for (int index = 0; index < bodies.size(); index++) {
                if (!near(bodies.get(index), unit)) continue;
                if (home == null) {
                    home = bodies.get(index);
                    home.add(unit);
                } else {
                    home.addAll(bodies.remove(index));
                    index--;
                }
            }
            if (home == null) {
                List<CommandUnitSnapshot> body = new ArrayList<>();
                body.add(unit);
                bodies.add(body);
            }
        }

        List<Cluster> clusters = new ArrayList<>(bodies.size());
        for (List<CommandUnitSnapshot> body : bodies) {
            clusters.add(build(snapshot, body));
        }
        return clusters;
    }

    private static boolean near(List<CommandUnitSnapshot> body, CommandUnitSnapshot unit) {
        for (CommandUnitSnapshot other : body) {
            double dx = other.position().getX() - unit.position().getX();
            double dz = other.position().getZ() - unit.position().getZ();
            if (dx * dx + dz * dz <= JOIN_DISTANCE_SQR) return true;
        }
        return false;
    }

    public static Cluster interpolated(Cluster cluster) {
        return build(cluster.snapshot(), cluster.body());
    }

    private static Cluster build(CommandGroupSnapshot snapshot, List<CommandUnitSnapshot> body) {
        double sumX = 0.0D;
        double sumZ = 0.0D;
        List<UUID> members = new ArrayList<>(body.size());
        for (CommandUnitSnapshot unit : body) {
            double x = MapPositionSmoothing.x(unit.unitId(), unit.position().getX());
            double z = MapPositionSmoothing.z(unit.unitId(), unit.position().getZ());
            sumX += x;
            sumZ += z;
            members.add(unit.unitId());
        }
        double centerX = sumX / body.size();
        double centerZ = sumZ / body.size();

        double reachNorth = 0.0D;
        int health = 0;
        int known = 0;
        int aggro = body.get(0).aggro();
        boolean shields = true;
        for (CommandUnitSnapshot unit : body) {
            double up = centerZ - MapPositionSmoothing.z(unit.unitId(), unit.position().getZ());
            if (up > reachNorth) reachNorth = up;
            if (unit.health() <= 100) {
                health += unit.health();
                known++;
            }
            if (unit.aggro() != aggro) aggro = MIXED;
            shields = shields && unit.shields();
        }
        return new Cluster(snapshot, List.copyOf(members), centerX, centerZ, reachNorth,
                known == 0 ? -1 : health / known, aggro, shields, List.copyOf(body));
    }

    public static int countOf(Cluster cluster, CommandUnitType type) {
        int count = 0;
        for (CommandUnitSnapshot unit : cluster.body()) {
            if (unit.type() == type) count++;
        }
        return count;
    }
}
