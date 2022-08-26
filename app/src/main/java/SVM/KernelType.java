package SVM;

public enum KernelType { 
    /// <summary>
    /// Linear: u'*v
    /// </summary>
    LINEAR(0), 
    /// <summary>
    /// Polynomial: (gamma*u'*v + coef0)^degree
    /// </summary>
    POLY(1), 
    /// <summary>
    /// Radial basis function: exp(-gamma*|u-v|^2)
    /// </summary>
    RBF(2), 
    /// <summary>
    /// Sigmoid: tanh(gamma*u'*v + coef0)
    /// </summary>
    SIGMOID(3),
    /// <summary>
    /// Precomputed kernel
    /// </summary>
    PRECOMPUTED(4);
    
    private final int value;
    private KernelType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
};
