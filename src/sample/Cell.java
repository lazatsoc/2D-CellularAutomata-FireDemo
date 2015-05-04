package sample;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Created by Lazaros on 3/5/2015.
 */
public class Cell extends Rectangle {

    BedType bed=BedType.Forest;
    float burned=0;
    float newburned=0;

    public Color getComputedFill() {
        return fill;
    }

    Color fill;

    public Cell(double width, double height) {
        super(width, height);
        setFill(Color.DARKGREEN);
    }

    public void setBed(BedType bed) {
        this.bed = bed;
        setFill(BedPaints[bed.ordinal()]);
        if (bed == BedType.Fire) setNewBurned(1);
        commit();
    }

    public void setNewBurned(float burned) {
        try {
            if ((double) burned > 1d) burned=1f;
            this.newburned = burned;
            Color bedcolor = BedPaints[bed.ordinal()];
            fill = Color.color(
                    bedcolor.getRed() * (1 - burned) + fullyBurned.getRed() * burned,
                    bedcolor.getGreen() * (1 - burned) + fullyBurned.getGreen() * burned,
                    bedcolor.getBlue() * (1 - burned) + fullyBurned.getBlue() * burned
            );
        }catch (Exception ex){
            ex.printStackTrace();
            int x=6;
        }
    }

    public void commit() {
        burned = newburned;
    }

    public float getBurned() {
        return burned;
    }

    public float getFlammablePercentage() {
        return flammablePercentages[bed.ordinal()];
    }

    public enum BedType {
        Water, Grass, DryGrass, Forest, Fire
    }
    private static float[] flammablePercentages = new float[] {-1f, -.2f, .3f, 0, 1f};
    public static Color[] BedPaints = new Color[] {Color.CORNFLOWERBLUE, Color.GREENYELLOW, Color.YELLOW, Color.DARKGREEN, Color.DARKRED.darker()};
    private static Color fullyBurned = Color.DARKRED.darker();
}
