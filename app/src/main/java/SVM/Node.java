package SVM;

public class Node 
{
	public int Index;
    double Value;
    
  /// <summary>
    /// Default Constructor.
    /// </summary>
    public Node()
    {
    }
    /// <summary>
    /// Constructor.
    /// </summary>
    /// <param name="index">The index of the value.</param>
    /// <param name="value">The value to store.</param>
    public Node(int index, double value)
    {
        Index = index;
        Value = value;
    }
    
    public String ToString()
    {
    	return Index + ":" + Value;
        
    }
}
