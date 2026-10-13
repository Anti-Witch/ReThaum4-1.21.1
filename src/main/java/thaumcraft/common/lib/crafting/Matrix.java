package thaumcraft.common.lib.crafting;

import thaumcraft.api.crafting.Part;

/**
 * Matrix class for 2D Part array rotation.
 * Used by DustTriggerMultiblock to rotate blueprints to match world orientation.
 * 
 * Ported to 1.20.1
 */
public class Matrix {
    
    int rows;
    int cols;
    Part[][] matrix;
    
    public Matrix(Part[][] matrix) {
        this.rows = matrix.length;
        this.cols = matrix[0].length;
        this.matrix = new Part[rows][cols];
        
        // Copy the input matrix
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                this.matrix[i][j] = matrix[i][j];
            }
        }
    }
    
    /**
     * Rotate the matrix 90 degrees clockwise the specified number of times.
     * 
     * @param times Number of 90-degree rotations (0-3)
     */
    public void Rotate90DegRight(int times) {
        for (int a = 0; a < times; ++a) {
            Part[][] newMatrix = new Part[cols][rows];
            
            for (int i = 0; i < rows; ++i) {
                for (int j = 0; j < cols; ++j) {
                    newMatrix[j][rows - i - 1] = matrix[i][j];
                }
            }
            
            matrix = newMatrix;
            
            // Swap dimensions
            int tmp = rows;
            rows = cols;
            cols = tmp;
        }
    }
    
    public Part[][] getMatrix() {
        return matrix;
    }
    
    public int getRows() {
        return rows;
    }
    
    public int getCols() {
        return cols;
    }
}




