

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

public class Imdb extends JPanel implements ActionListener
{
    
    private Files files;
    private File[] directories;
    private ArrayList<ImdbInfo> movieInfo;
    private JFrame frame;
    private DefaultTableModel model;
    
    private JLayeredPane layeredPane;
    private JFileChooser fc;
    private BufferedImage img;
    private JButton openButton;
    private JButton randomButton;
    public JLabel label;
    private ArrayList<JLabel> jlabels;
    
    private int folderCount = 0;
    
    public Imdb()
    {
        movieInfo = new ArrayList<>();
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setPreferredSize(new Dimension(640, 480));
        
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(createPanel());


    }
    
    private void buildUI()
    {
        frame = new JFrame("IMDB Info");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        try 
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } 
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) 
        {
            ex.printStackTrace();
        }
        JComponent newContentPane = new Imdb();
        newContentPane.setOpaque(true);
        frame.setContentPane(newContentPane);
        frame.pack();
        frame.setVisible(true);
    }
    
    private JPanel createPanel()
    {
        JPanel controls = new JPanel();
        openButton = new JButton("Open folder");
        openButton.addActionListener(this);
        controls.add(openButton);
        label = new JLabel("No folder selected.");
        controls.add(label);
        return controls;
    }
    
    private String readUrl(String urlString) throws IOException
    {
        BufferedReader reader = null;
        try 
        {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200)
            {
                return "";
            }
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder buffer = new StringBuilder();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
            {
                buffer.append(chars, 0, read);
            }
            return buffer.toString();
        } 
        finally 
        {
            if (reader != null)
            {
                reader.close();
            }
        }
    }
    
    private String bingSearch(String searchQuery)
    {
        String id = "";
        try 
        {
            final String accountKey = AuthVariables.bingAccountKey;
            final String bingUrlPattern = "https://api.datamarket.azure.com/Bing/Search/Web?Query=%%27%s%%27&$format=JSON&$top=1";

            final String query = URLEncoder.encode(searchQuery, Charset.defaultCharset().name());
            final String bingUrl = String.format(bingUrlPattern, query);            
            
            final String accountKeyEnc = Base64.getEncoder().encodeToString((accountKey + ":" + accountKey).getBytes());
            
            final URL url = new URL(bingUrl);
            final URLConnection connection = url.openConnection();
            connection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);
            try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) 
            {
                String inputLine;
                final StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) 
                {
                    response.append(inputLine);
                }
                try
                {
                    JsonParser jsonParser = new JsonParser();
                    JsonObject searchInfo = jsonParser.parse(response.toString())
                        .getAsJsonObject().getAsJsonObject("d")
                        .getAsJsonObject().getAsJsonArray("results").get(0)
                        .getAsJsonObject();

                    String displayUrl = searchInfo.get("DisplayUrl").getAsString();
                    String title = "title/";
                    int position = displayUrl.indexOf(title);
                    if (position != -1)
                    {
                        String tmp = displayUrl.substring(position + title.length());
                        if (tmp.contains("/"))
                        {
                            id = tmp.substring(0, tmp.indexOf('/'));
                        }
                        else
                        {
                            id = tmp;
                        }
                    }
                }
                catch (JsonSyntaxException | java.lang.IndexOutOfBoundsException e)
                {
                    return "";
                }
                
            }
        } 
        catch (MalformedURLException ex) 
        {
            Logger.getLogger(Imdb.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IOException e)
        {
            Logger.getLogger(Imdb.class.getName()).log(Level.SEVERE, null, e);
        }
        
        return id;
    }
    
    
    private void listFolders(String path)
    {
        directories = new File(path).listFiles(File::isDirectory);
        folderCount = directories.length;
        System.out.println("Found " + folderCount + " folders.");
        cleanNames();
        /*
        for (File file : directories)
        {
            System.out.println(file.getName());
        }
        */
    }
    
    private void fillTable()    
    {
        int i = 0;
        int size = movieInfo.size();
        model = new DefaultTableModel(new String[]{"Title", "Genre", "Year", "Rating", "Link"}, 0);

        for (ImdbInfo info : movieInfo)
        {
            i++;
            try 
            {
                String json = "";
                if (info.getYear() != 0)
                {
                    json = readUrl("http://www.omdbapi.com/?t=" + URLEncoder.encode(info.getName(), "UTF-8") + "&y=" + Integer.toString(info.getYear()) + "&type=movie");
                }
                else
                {
                    json = readUrl("http://www.omdbapi.com/?t=" + URLEncoder.encode(info.getName(), "UTF-8"));
                }
                //System.out.println(json);
                if (!json.isEmpty())
                {

                    JsonParser parser = new JsonParser();
                    JsonObject o = parser.parse(json).getAsJsonObject();

                    if (o.get("Response").getAsString().equals("True"))
                    {
                        if (o.get("Type").getAsString().equals("movie"))
                        {
                            String title = o.get("Title").getAsString();
                            String genre = o.get("Genre").getAsString();
                            String yearTmp = o.get("Year").getAsString();
                            int year = 0;
                            if (yearTmp.length() > 4)
                            {
                                year = Integer.parseInt(yearTmp.substring(0, 4));
                            }
                            else
                            {
                                year = o.get("Year").getAsInt();
                            }
                            float rating = 0;
                            if (!o.get("imdbRating").getAsString().equals("N/A"))
                            {
                                rating = o.get("imdbRating").getAsFloat();
                            }
                            else
                            {
                                System.out.println("Not enough votes: " + info.getName());
                            }
                            String link = "http://www.imdb.com/title/" + o.get("imdbID").getAsString();
                            System.out.println(i + "/" + size + ": " + title);
                             model.addRow(new Object[]{title, genre, year, rating, link});
                        }
                    
                    }
                    else
                    {
                        System.out.println("Searching: " + info.getName() + " from Bing...");
                        String id = bingSearch("site:imdb.com instreamset:(url):/title/tt " + info.getName());
                        
                        if (id.isEmpty())
                        {
                            model.addRow(new Object[]{info.getName(), "NOT FOUND", info.getYear(), 0, ""});
                            System.out.println(i + "/" + size + ": NOT FOUND: " + info.getName());
                        }
                        else
                        {
                            json = readUrl("http://www.omdbapi.com/?i=" + URLEncoder.encode(id, "UTF-8") + "&type=movie");
                            if (!json.isEmpty())
                            {

                                parser = new JsonParser();
                                o = parser.parse(json).getAsJsonObject();

                                if (o.get("Response").getAsString().equals("True"))
                                {
                                    if (o.get("Type").getAsString().equals("movie"))
                                    {
                                        String title = o.get("Title").getAsString();
                                        String genre = o.get("Genre").getAsString();
                                        String yearTmp = o.get("Year").getAsString();
                                        int year = 0;
                                        if (yearTmp.length() > 4)
                                        {
                                            year = Integer.parseInt(yearTmp.substring(0, 4));
                                        }
                                        else
                                        {
                                            year = o.get("Year").getAsInt();
                                        }
                                        float rating = 0;
                                        if (!o.get("imdbRating").getAsString().equals("N/A"))
                                        {
                                            rating = o.get("imdbRating").getAsFloat();
                                        }
                                        else
                                        {
                                            System.out.println("Not enough votes: " + info.getName());
                                        }
                                        String link = "http://www.imdb.com/title/" + o.get("imdbID").getAsString();
                                        System.out.println(i + "/" + size + ": FOUND: " + title);
                                        model.addRow(new Object[]{title, genre, year, rating, link});
                                    }
                                }
                            }
                        }
                    }
                }                       
            } 
            catch (UnsupportedEncodingException ex) 
            {
                Logger.getLogger(Imdb.class.getName()).log(Level.SEVERE, null, ex);
            }
            catch (IOException ex) 
            {
                Logger.getLogger(Imdb.class.getName()).log(Level.SEVERE, null, ex);
            }
        } 
        
        JTable table = new JTable(model);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(2, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        table.addMouseListener(new MouseAdapter() 
        {
            public void mousePressed(MouseEvent e) 
            {
                if (e.isControlDown()) 
                {
                    try 
                    {
                        JTable target = (JTable)e.getSource();
                        int row = target.rowAtPoint(e.getPoint());
                        String value = (String)target.getValueAt(row, 4);
                        if (value != null || !value.isEmpty())
                        {
                            openWebpage(new URL(value));
                        }
                    } 
                    catch (MalformedURLException ex) 
                    {
                        Logger.getLogger(Imdb.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        this.add(new JScrollPane(table));
        this.revalidate();
        this.repaint();
    }
    
    private void cleanNames()
    {
        for (File file : directories)
        {
            if (file.getName().contains("(") && file.getName().contains(")"))
            {
                System.out.println(file.getName());
                try 
                {
                    int year = Integer.parseInt(file.getName().substring(file.getName().indexOf("(") + 1, file.getName().indexOf(")")));
                    ImdbInfo o1 = new ImdbInfo(file.getName().substring(0, file.getName().indexOf("(") - 1), year);
                    movieInfo.add(o1);
                }
                catch (java.lang.NumberFormatException e)
                {
                    noBrackets(file);
                }
            }
            else if (file.getName().contains("[") && file.getName().contains("]"))
            {
                System.out.println(file.getName());
                try 
                {
                    int year = Integer.parseInt(file.getName().substring(file.getName().indexOf("[") + 1, file.getName().indexOf("]")));
                    ImdbInfo o1 = new ImdbInfo(file.getName().substring(0, file.getName().indexOf("[") - 1), year);
                    movieInfo.add(o1);
                }
                catch (java.lang.NumberFormatException e)
                {
                    noBrackets(file);
                }
            }
            else
            {
                noBrackets(file);
            }
        }
        System.out.println("Found " + movieInfo.size() + " movies.");
    }
    
    private void noBrackets(File file)
    {
        String tmp = file.getName().replace(".", " ").replace("-", " ").replace("_", " ").replace("(", " ").replace(")", " ").replace("[", " ").replace("]", " ");
        String[] splited = tmp.split("\\s+");
        String name = "";
        int year = 0;
        for (String s : splited)
        {
            if (s.equalsIgnoreCase("720") || s.equalsIgnoreCase("720p") 
                     || s.equalsIgnoreCase("unrated") || s.equalsIgnoreCase("1080") 
                     || s.equalsIgnoreCase("1080p")|| s.equalsIgnoreCase("limited")
                     || s.equalsIgnoreCase("bluray") || s.equalsIgnoreCase("brrip")
                     || s.equalsIgnoreCase("hd") || s.equalsIgnoreCase("mp4")
                     || s.equalsIgnoreCase("x264") || s.equalsIgnoreCase("AAC")
                     || s.equalsIgnoreCase("dvdrip") || s.equalsIgnoreCase("xvid")
                     || s.equalsIgnoreCase("divx") || s.equalsIgnoreCase("KLAXXON")
                     || s.equalsIgnoreCase("yifi") || s.equalsIgnoreCase("multisub")
                     || s.equalsIgnoreCase("r5") || s.equalsIgnoreCase("imdb")
                     || s.equalsIgnoreCase("eng") || s.equalsIgnoreCase("retail")
                     || s.equalsIgnoreCase("FXG") || s.equalsIgnoreCase("imdb")
                     || s.equalsIgnoreCase("PAL") || s.equalsIgnoreCase("DVD9")
                     || s.equalsIgnoreCase("ac3") || s.equalsIgnoreCase("5,1")
                     || s.equalsIgnoreCase("5.1") || s.equalsIgnoreCase("Realuploads")
                     || s.equalsIgnoreCase("axxo") || s.equalsIgnoreCase("nordic")
                     || s.toLowerCase().contains("s0") || s.toLowerCase().contains("e0"))
            {
                break;
            }
            if (s.length() == 4)
            {
                try
                {
                    year = Integer.parseInt(s);
                    break;
                }
                catch (NumberFormatException e)
                {
                    name += s + " ";
                }
            }
            
            else
            {
                name += s + " ";
            }
        }
        if (name.length() > 0 && name.charAt(name.length() - 1) == ' ')
        {
            name = name.substring(0, name.length() - 1);
        }
        ImdbInfo o1 = new ImdbInfo(name, year);
        movieInfo.add(o1);
    }
    
    public void openWebpage(URI uri) 
    {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try 
            {
                desktop.browse(uri);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public void openWebpage(URL url) 
    {
        try 
        {
            openWebpage(url.toURI());
        } 
        catch (URISyntaxException e) 
        {
            e.printStackTrace();
        }
    }
    
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == openButton)
        {
            fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setCurrentDirectory(fc.getFileSystemView().getParentDirectory(new File("C:\\")));
            fc.showOpenDialog(null);
            if (fc.getSelectedFile() != null)
            {
                label.setText(fc.getSelectedFile().getAbsolutePath());
                listFolders(fc.getSelectedFile().getAbsolutePath());
                fillTable();
                Random r = new Random();
                int random = r.nextInt(movieInfo.size());
                System.out.println("Random: " + movieInfo.get(random).getName());
                movieInfo.clear();
            }
        }
    }
    
    public static void main(String[] args) throws IOException
    {
        Imdb imdb = new Imdb();
        imdb.buildUI();
        
    }
}
    
