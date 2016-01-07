
public class ImdbInfo
{
    private String name;
    private int year;
    private float rating;

    ImdbInfo(String name, int year) 
    {
        this.name = name;
        this.year = year;
        this.rating = 0;
    }

    public String getName()
    {
        return this.name;
    }
    
    public int getYear()
    {
        return this.year;
    }
    
    @Override
    public String toString() 
    { 
        return this.name + " " + Integer.toString(this.year);
    } 
}
