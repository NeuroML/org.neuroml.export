package org.neuroml.export.svg;

// By: Ken Perlin
// Obtained from: http://mrl.nyu.edu/~perlin/java/Matrix3D.html

// Homogeneous transformation matrices in three dimensions
public class Matrix3D extends MatrixN
{

    Matrix3D() { // create a new identity transformation
        super(4);
        identity();
    }

    void rotateX(double theta) { // rotate transformation about the X axis

        Matrix3D tmp = new Matrix3D();
        double c = Math.cos(theta);
        double s = Math.sin(theta);

        tmp.set(1, 1, c);
        tmp.set(1, 2, -s);
        tmp.set(2, 1, s);
        tmp.set(2, 2, c);

        preMultiply(tmp);
    }

    void rotateY(double theta) { // rotate transformation about the Y axis

        Matrix3D tmp = new Matrix3D();
        double c = Math.cos(theta);
        double s = Math.sin(theta);

        tmp.set(2, 2, c);
        tmp.set(2, 0, -s);
        tmp.set(0, 2, s);
        tmp.set(0, 0, c);

        preMultiply(tmp);
    }

    void rotateZ(double theta) { // rotate transformation about the Z axis

        Matrix3D tmp = new Matrix3D();
        double c = Math.cos(theta);
        double s = Math.sin(theta);

        tmp.set(0, 0, c);
        tmp.set(0, 1, -s);
        tmp.set(1, 0, s);
        tmp.set(1, 1, c);

        preMultiply(tmp);
    }

    void translate(double a, double b, double c) { // translate

        Matrix3D tmp = new Matrix3D();

        tmp.set(0, 3, a);
        tmp.set(1, 3, b);
        tmp.set(2, 3, c);

        preMultiply(tmp);
    }

    void translate(Vector3D v) {
        translate(v.get(0), v.get(1), v.get(2));
    }

    void scale(double s) { // scale uniformly

        Matrix3D tmp = new Matrix3D();

        tmp.set(0, 0, s);
        tmp.set(1, 1, s);
        tmp.set(2, 2, s);

        preMultiply(tmp);
    }

    void scale(double r, double s, double t) { // scale non-uniformly

        Matrix3D tmp = new Matrix3D();

        tmp.set(0, 0, r);
        tmp.set(1, 1, s);
        tmp.set(2, 2, t);

        preMultiply(tmp);
    }

    void scale(Vector3D v) {
        scale(v.get(0), v.get(1), v.get(2));
    }
}

// Homogeneous vectors in three dimensions

class Vector3D extends VectorN
{

    public Vector3D()
    {
        super(4);
    }

    public Vector3D(double x, double y, double z)
    {
        super(4);
        set(x,y,z);
    }

    public Vector3D(VectorN source)
    {
        super(4);
        set(source.get(0), source.get(1), source.get(2), source.get(3));
    }

    // create a new 3D homogeneous vector
    void set(double x, double y, double z, double w) {          // set value of vector
        set(0, x);
        set(1, y);
        set(2, z);
        set(3, w);
    }

    void set(double x, double y, double z) {
        set(x, y, z, 1);
    } // set value of a 3D point
}

// Geometric vectors of size N

class VectorN {
    private double v[];

    VectorN(int n) {
        v = new double[n];
    }    // create a new vector

    int size() {
        return v.length;
    }          // return vector size

    double getX()
    {
        return get(0);
    }

    double getY()
    {
        return get(1);
    }

    double getZ()
    {
        return get(2);
    }

    double get(int j) {
        return v[j];
    }       // get one element

    void set(int j, double f) {
        v[j] = f;
    }  // set one element

    void set(VectorN vec) {                  // copy from another vector
        for (int j = 0; j < size(); j++)
            set(j, vec.get(j));
    }

    public String toString() {               // convert to string representation
        String s = "{";
        for (int j = 0; j < size(); j++)
            s += (j == 0 ? "" : ",") + get(j);
        return s + "}";
    }

    VectorN transform(MatrixN mat) {            // multiply by an N × N matrix

        VectorN result = new VectorN(size());
        double f;

        for (int i = 0; i < size(); i++) {
            f = 0.;
            for (int j = 0; j < size(); j++)
                f += mat.get(i, j) * get(j);
            result.set(i, f);
        }

        return result;
    }

    double distance(VectorN vec) {          // euclidean distance
        double x, y, d = 0;
        for (int i = 0; i < size(); i++) {
            x = vec.get(0) - get(0);
            y = vec.get(1) - get(1);
            d += x * x + y * y;
        }
        return Math.sqrt(d);
    }
}

// Geometric matrices of size N × N

class MatrixN { // N × N matrices
    private VectorN v[];

    MatrixN(int n) {                                    // make a new square matrix
        v = new VectorN[n];
        for (int i = 0; i < n; i++)
            v[i] = new VectorN(n);
    }

    int size() {
        return v.length;
    }                     // return no. of rows

    double get(int i, int j) {
        return get(i).get(j);
    }  // get one element

    void set(int i, int j, double f) {
        v[i].set(j, f);
    } // set one element

    VectorN get(int i) {
        return v[i];
    }                 // get one row

    void set(int i, VectorN vec) {
        v[i].set(vec);
    }     // set one row

    void set(MatrixN mat) {                             // copy from another matrix
        for (int i = 0; i < size(); i++)
            set(i, mat.get(i));
    }

    public String toString() {                   // convert to string representation
        String s = "{";
        for (int i = 0; i < size(); i++)
            s += (i == 0 ? "" : ",") + get(i);
        return s + "}";
    }

    void identity() {                            // set to identity matrix
        for (int j = 0; j < size(); j++)
            for (int i = 0; i < size(); i++)
                set(i, j, (i == j ? 1 : 0));
    }

    void preMultiply(MatrixN mat) {              // mat × this
        MatrixN tmp = new MatrixN(size());
        double f;

        for (int j = 0; j < size(); j++)
            for (int i = 0; i < size(); i++) {
                f = 0.;
                for (int k = 0; k < size(); k++)
                    f += mat.get(i, k) * get(k, j);
                tmp.set(i, j, f);
            }
        set(tmp);
    }

    void postMultiply(MatrixN mat) {             // this × mat
        MatrixN tmp = new MatrixN(size());
        double f;

        for (int j = 0; j < size(); j++)
            for (int i = 0; i < size(); i++) {
                f = 0.;
                for (int k = 0; k < size(); k++)
                    f += get(i, k) * mat.get(k, j);
                tmp.set(i, j, f);
            }
        set(tmp);
    }
}
