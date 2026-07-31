import com.google.gson.*;
import java.io.*;
import java.nio.file.*;

/**
 * Reads a Blockbench Java Block/Item export and bakes multi-axis
 * rotations into the element coordinates, producing a valid vanilla
 * Minecraft Java item model.
 *
 * Multi-axis rotations (x/y/z Euler) are applied in Blockbench's
 * YXZ order to the from/to corners, and face assignments are remapped.
 */
public class BakeRotations {

    public static void main(String[] args) throws Exception {
        String inputPath = args.length > 0 ? args[0]
                : "c:/idk/jttw_engine_custom/assets_source/Staff.json";
        String outputPath = args.length > 1 ? args[1]
                : "c:/idk/jttw_engine_custom/src/main/resources/assets/jttw_engine/models/item/ruyi_jingu_bang.json";

        String json = Files.readString(Path.of(inputPath));
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        // Remove invalid fields
        root.remove("format_version");

        // Add parent for item rendering
        root.addProperty("parent", "minecraft:block/block");

        // Fix texture references
        JsonObject textures = root.getAsJsonObject("textures");
        if (textures.has("1")) {
            textures.addProperty("1", "jttw_engine:item/ruyi_jingu_bang_bands");
        }
        if (textures.has("12")) {
            textures.addProperty("12", "jttw_engine:item/ruyi_jingu_bang_base");
        }
        if (textures.has("particle")) {
            textures.addProperty("particle", "jttw_engine:item/ruyi_jingu_bang_base");
        }

        // Process elements
        JsonArray elements = root.getAsJsonArray("elements");
        for (int i = 0; i < elements.size(); i++) {
            JsonObject elem = elements.get(i).getAsJsonObject();
            if (!elem.has("rotation")) continue;

            JsonElement rotElem = elem.get("rotation");
            if (!rotElem.isJsonObject()) continue;

            JsonObject rot = rotElem.getAsJsonObject();

            // Already valid vanilla rotation?
            if (rot.has("angle") && rot.has("axis")) continue;

            // Multi-axis rotation — bake it
            double rx = rot.has("x") ? rot.get("x").getAsDouble() : 0;
            double ry = rot.has("y") ? rot.get("y").getAsDouble() : 0;
            double rz = rot.has("z") ? rot.get("z").getAsDouble() : 0;

            double ox = 0, oy = 0, oz = 0;
            if (rot.has("origin")) {
                JsonArray origin = rot.getAsJsonArray("origin");
                ox = origin.get(0).getAsDouble();
                oy = origin.get(1).getAsDouble();
                oz = origin.get(2).getAsDouble();
            }

            // Get from/to
            JsonArray fromArr = elem.getAsJsonArray("from");
            JsonArray toArr = elem.getAsJsonArray("to");
            double[] from = {fromArr.get(0).getAsDouble(), fromArr.get(1).getAsDouble(), fromArr.get(2).getAsDouble()};
            double[] to = {toArr.get(0).getAsDouble(), toArr.get(1).getAsDouble(), toArr.get(2).getAsDouble()};

            // All 8 corners of the box
            double[][] corners = new double[8][3];
            int ci = 0;
            for (int xi = 0; xi < 2; xi++)
                for (int yi = 0; yi < 2; yi++)
                    for (int zi = 0; zi < 2; zi++)
                        corners[ci++] = new double[]{
                                xi == 0 ? from[0] : to[0],
                                yi == 0 ? from[1] : to[1],
                                zi == 0 ? from[2] : to[2]
                        };

            // Apply rotation to all corners (Blockbench YXZ order)
            for (int c = 0; c < 8; c++) {
                double dx = corners[c][0] - ox;
                double dy = corners[c][1] - oy;
                double dz = corners[c][2] - oz;

                // Ry
                if (ry != 0) {
                    double rad = Math.toRadians(ry);
                    double cos = Math.cos(rad), sin = Math.sin(rad);
                    double nx = dx * cos + dz * sin;
                    double nz = -dx * sin + dz * cos;
                    dx = nx; dz = nz;
                }
                // Rx
                if (rx != 0) {
                    double rad = Math.toRadians(rx);
                    double cos = Math.cos(rad), sin = Math.sin(rad);
                    double ny = dy * cos - dz * sin;
                    double nz = dy * sin + dz * cos;
                    dy = ny; dz = nz;
                }
                // Rz
                if (rz != 0) {
                    double rad = Math.toRadians(rz);
                    double cos = Math.cos(rad), sin = Math.sin(rad);
                    double nx = dx * cos - dy * sin;
                    double ny = dx * sin + dy * cos;
                    dx = nx; dy = ny;
                }

                corners[c][0] = round4(dx + ox);
                corners[c][1] = round4(dy + oy);
                corners[c][2] = round4(dz + oz);
            }

            // New AABB
            double[] newFrom = {corners[0][0], corners[0][1], corners[0][2]};
            double[] newTo = {corners[0][0], corners[0][1], corners[0][2]};
            for (int c = 1; c < 8; c++) {
                for (int a = 0; a < 3; a++) {
                    newFrom[a] = Math.min(newFrom[a], corners[c][a]);
                    newTo[a] = Math.max(newTo[a], corners[c][a]);
                }
            }

            // Update from/to
            elem.add("from", makeArray(newFrom));
            elem.add("to", makeArray(newTo));

            // Remap faces based on rotation
            JsonObject faces = elem.getAsJsonObject("faces");
            JsonObject newFaces = remapFaces(faces, rx, ry, rz);
            elem.add("faces", newFaces);

            // Replace rotation with valid vanilla one
            JsonObject newRot = new JsonObject();
            newRot.addProperty("angle", 0);
            newRot.addProperty("axis", "y");
            newRot.add("origin", makeArray(new double[]{ox, oy, oz}));
            elem.add("rotation", newRot);
        }

        // Remove groups (Blockbench metadata)
        root.remove("groups");

        // Write output
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Files.writeString(Path.of(outputPath), gson.toJson(root));
        System.out.println("Done! Wrote " + elements.size() + " elements to " + outputPath);
    }

    static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    static JsonArray makeArray(double[] vals) {
        JsonArray arr = new JsonArray();
        for (double v : vals) arr.add(round4(v));
        return arr;
    }

    /**
     * Remap face keys after rotation.
     * Face normals: north=-Z, south=+Z, east=+X, west=-X, up=+Y, down=-Y
     */
    static JsonObject remapFaces(JsonObject faces, double rx, double ry, double rz) {
        String[] faceNames = {"north", "south", "east", "west", "up", "down"};
        // Normal vectors for each face
        double[][] normals = {
                {0, 0, -1},  // north
                {0, 0, 1},   // south
                {1, 0, 0},   // east
                {-1, 0, 0},  // west
                {0, 1, 0},   // up
                {0, -1, 0}   // down
        };

        // Rotate each normal
        String[] mapping = new String[6];
        for (int f = 0; f < 6; f++) {
            double dx = normals[f][0], dy = normals[f][1], dz = normals[f][2];

            // Apply same YXZ rotation
            if (ry != 0) {
                double rad = Math.toRadians(ry);
                double cos = Math.cos(rad), sin = Math.sin(rad);
                double nx = dx * cos + dz * sin;
                double nz = -dx * sin + dz * cos;
                dx = nx; dz = nz;
            }
            if (rx != 0) {
                double rad = Math.toRadians(rx);
                double cos = Math.cos(rad), sin = Math.sin(rad);
                double ny = dy * cos - dz * sin;
                double nz = dy * sin + dz * cos;
                dy = ny; dz = nz;
            }
            if (rz != 0) {
                double rad = Math.toRadians(rz);
                double cos = Math.cos(rad), sin = Math.sin(rad);
                double nx = dx * cos - dy * sin;
                double ny = dx * sin + dy * cos;
                dx = nx; dy = ny;
            }

            // Find which face this normal now points to
            mapping[f] = normalToFace(round4(dx), round4(dy), round4(dz));
        }

        // Build new faces object
        JsonObject result = new JsonObject();
        for (int f = 0; f < 6; f++) {
            String origFace = faceNames[f];
            String newFace = mapping[f];
            if (faces.has(origFace)) {
                result.add(newFace, faces.get(origFace).deepCopy());
            }
        }
        return result;
    }

    static String normalToFace(double x, double y, double z) {
        // Find dominant axis
        double ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
        if (ax >= ay && ax >= az) {
            return x > 0 ? "east" : "west";
        } else if (ay >= ax && ay >= az) {
            return y > 0 ? "up" : "down";
        } else {
            return z > 0 ? "south" : "north";
        }
    }
}
