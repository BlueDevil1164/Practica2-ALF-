import java.io.*;
import java.util.*;

public class CYK{

    public static String word;
    public static String startingSymbol;
    public static boolean isTokenWord = false;
    public static ArrayList<String> terminals = new ArrayList<String>();
    public static ArrayList<String> nonTerminals = new ArrayList<String>();
    public static TreeMap<String,ArrayList<String>> grammar = new TreeMap<>();

    public static void main(String[] args){
        if(args.length < 2){
            System.out.println("Uso: java CYK <Archivo> <Palabra>.");
            System.exit(1);
        }else if (args.length > 2){
            isTokenWord = true;
        }
        doSteps(args);
    }

    public static void doSteps(String[] args){
        parseGrammar(args);
        String[][] cykTable = createCYKTable();
        printResult(doCyk(cykTable));
    }

    public static void parseGrammar(String[] args){
        Scanner input = openFile(args[0]);
        ArrayList<String> tmp = new ArrayList<>();
        int line = 2;

        word = getWord(args);
        startingSymbol = input.next();
        input.nextLine();

        while(input.hasNextLine() && line <= 3){
            tmp.addAll(Arrays.<String>asList(toArray(input.nextLine())));
            if(line == 2) { terminals.addAll(tmp); }
            if(line == 3) { nonTerminals.addAll(tmp); }
            tmp.clear();
            line++;
        }

        while(input.hasNextLine()){
            tmp.addAll(Arrays.<String>asList(toArray(input.nextLine())));
            String leftSide = tmp.get(0);
            tmp.remove(0);
            grammar.put(leftSide, new ArrayList<String>());
            grammar.get(leftSide).addAll(tmp);
            tmp.clear();
        }
        input.close();
    }

    public static String getWord(String[] args){
        if(!isTokenWord) { return args[1]; }
        String[] argsWithoutFile = new String[args.length - 1];
        for(int i = 1; i < args.length; i++){
            argsWithoutFile[i-1] = args[i];
        }
        return toString(argsWithoutFile);
    }

    public static void printResult (String[][] cykTable){
        System.out.println("Palabra: " + word);
        System.out.println("\nG = (" + terminals.toString().replace("[", "{").replace("]", "}")
                          + ", " + nonTerminals.toString().replace("[", "{").replace("]", "}")
                          + ", P, " + startingSymbol + ")\n\nCon Produciones P como:");
        for(String s: grammar.keySet()){
            System.out.println(s + " -> " + grammar.get(s).toString().replaceAll("[\\[\\]\\,]", "").replaceAll("\\s", " | "));
        }
        System.out.println("\nAplicación del algoritmo CYK:\n");
        drawTable(cykTable);
    }

    public static void drawTable(String[][] cykTable){
        int l = findLongestString(cykTable) + 2;
        String formatString = "| %-" + l + "s ";
        String s = "";
        StringBuilder sb = new StringBuilder();

    // Módulos de estructura de mesa de construcción
        sb.append("+");
        for(int x = 0; x <= l + 2; x++){
            if(x == l + 2){
                sb.append("+");
            }else{
                sb.append("-");
            }
        }
        String low = sb.toString();
        sb.delete(0, 1);
        String lowRight = sb.toString();

    // Imprimir tabla
        for(int i = 0; i < cykTable.length; i++){
            for(int j = 0; j <= cykTable[i].length; j++){
                System.out.print((j == 0) ? low : (i <= 1 && j == cykTable[i].length - 1) ? "" : lowRight);
            }
            System.out.println();
            for(int j = 0; j < cykTable[i].length; j++){
                s = (cykTable[i][j].isEmpty()) ? "-" : cykTable[i][j];
                System.out.format(formatString, s.replaceAll("\\s", ","));
                if(j == cykTable[i].length - 1) { System.out.print("|"); }
            }
            System.out.println();
        }
        System.out.println(low+"\n");

    // Evaluar el éxito.
        if(cykTable[cykTable.length-1][cykTable[cykTable.length-1].length-1].contains(startingSymbol)){
            System.out.println("La Palabra \"" + word + "\" es un elemento del CFG G y puede derivarse de él.");
        }else{
            System.out.println("La Palabra \"" + word + "\" no es un elemento del CFG G y no puede derivarse de él.");
        }
    }

    public static int findLongestString(String[][] cykTable){
        int x = 0;
        for(String[] s : cykTable){
            for(String d : s){
                if(d.length() > x){ x = d.length(); }
            }
        }
        return x;
    }


    // Matriz irregular para el algoritmo
    public static String[][] createCYKTable (){
        int length = isTokenWord ? toArray(word).length : word.length();
        String[][] cykTable = new String[length + 1][];
        cykTable[0] = new String[length];
        for(int i = 1; i < cykTable.length; i++){
            cykTable[i] = new String[length - (i - 1)];
        }
        for(int i = 1; i < cykTable.length; i++){
            for(int j = 0; j < cykTable[i].length; j++){
                cykTable[i][j] = "";
            }
        }
        return cykTable;
    }

    public static String[][] doCyk(String[][] cykTable){
  // Paso 1: rellenar la fila del encabezado
        for(int i = 0; i < cykTable[0].length; i++){
            cykTable[0][i] = manageWord(word, i);
        }

    // Paso 2: Obtenga producciones para terminales
        for(int i = 0; i < cykTable[1].length; i++){
            String[] validCombinations = checkIfProduces(new String[] {cykTable[0][i]});
            cykTable[1][i] = toString(validCombinations);
        }
        if(word.length() <= 1) { return cykTable; }
    // Paso 3: Obtenga producciones para subpalabras con una longitud de 2
        for(int i = 0; i < cykTable[2].length; i++){
            String[] downwards = toArray(cykTable[1][i]);
            String[] diagonal = toArray(cykTable[1][i+1]);
            String[] validCombinations = checkIfProduces(getAllCombinations(downwards, diagonal));
            cykTable[2][i] = toString(validCombinations);
        }
        if(word.length() <= 2){ return cykTable; }
    // Paso 4: Obtenga producciones para subpalabras con la longitud de n
        TreeSet<String> currentValues = new TreeSet<String>();

        for(int i = 3; i < cykTable.length; i++){
            for(int j = 0; j < cykTable[i].length; j++){
                for(int compareFrom = 1; compareFrom < i; compareFrom++){
                    String[] downwards = cykTable[compareFrom][j].split("\\s");
                    String[] diagonal = cykTable[i-compareFrom][j+compareFrom].split("\\s");
                    String[] combinations = getAllCombinations(downwards, diagonal);
                    String[] validCombinations = checkIfProduces(combinations);
                    if(cykTable[i][j].isEmpty()){
                        cykTable[i][j] = toString(validCombinations);
                    }else{
                        String[] oldValues = toArray(cykTable[i][j]);
                        ArrayList<String> newValues = new ArrayList<String>(Arrays.asList(oldValues));
                        newValues.addAll(Arrays.asList(validCombinations));
                        currentValues.addAll(newValues);
                        cykTable[i][j] = toString(currentValues.toArray(new String[currentValues.size()]));
                    }
                }
                currentValues.clear();
            }
        }
        return cykTable;
    }

    public static String manageWord(String word, int position){
        if(!isTokenWord){ return Character.toString(word.charAt(position)); }
        return toArray(word)[position];
    }

    public static String[] checkIfProduces(String[] toCheck){
        ArrayList<String> storage = new ArrayList<>();
        for(String s : grammar.keySet()){
            for(String current : toCheck){
                if(grammar.get(s).contains(current)){
                    storage.add(s);
                }
            }
        }
        if(storage.size() == 0) { return new String[] {}; }
        return storage.toArray(new String[storage.size()]);
    }

    public static String[] getAllCombinations(String[] from, String[] to){
        int length = from.length * to.length;
        int counter = 0;
        String[] combinations = new String[length];
        if(length == 0){ return combinations; };
        for(int i = 0; i < from.length; i++){
            for(int j = 0; j < to.length; j++){
                combinations[counter] = from[i] + to[j];
                counter++;
            }
        }
        return combinations;
    }

    public static String toString(String[] input){
        return Arrays.toString(input).replaceAll("[\\[\\]\\,]", "");
    }

    public static String[] toArray(String input){
        return input.split("\\s");
    }

    public static Scanner openFile(String file){
        try{
            return new Scanner(new File(file));
        }catch(FileNotFoundException e){
            System.out.println("Error: no puedo encontrar o abrir el archivo: " + file + ".");
            System.exit(1);
            return null;
        }
    }
}
