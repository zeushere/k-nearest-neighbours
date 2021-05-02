import java.io.IOException;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class KNNProgram {
    private static final String COMMA_DELIMITER = ",";

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        //Domyslnie ustawiona liczba najblizszych sasiadow, ktora bedzie zmieniana przez uzytkownika z klawiatury
        int k = 0;

        System.out.println("-------------------------------------------");

        System.out.println("Witaj w programie bedacym implementacja algorytmu K-Najblizszych Sasiadow");
        System.out.println("Odleglosc pomiedzy punktami bedzie liczona za pomoca odleglosci Euklidesa");
        System.out.println("Podaj liczbę najblizszych sasiadow (k)");

        try {
            k = scanner.nextInt();
            if (k < 1) {
                throw new Exception();
            }
        } catch (Exception ex) {
            boolean wrongData = true;
            while (wrongData) {
                System.out.println("Wprowadzono bledne k!");
                System.out.println("Sprobuj ponownie!");
                k = scanner.nextInt();
                if (k > 0) wrongData = false;
            }

        }

        System.out.println("Program zaraz rozpocznie wczytywanie plikow, zaczynamy odliczanie czasu!");
        System.out.println();

        //Zmienna przechowujaca ms od poczatku trwania logiki programu
        long timeMilli = Calendar.getInstance().getTimeInMillis();

        //Wczytanie danych obiektów treningowych (W CELU SPRAWDZENIA DLA ECOLI WYSTARCZY ZMIENIC NAZWE W PONIZSZYM CSV READER NA "ecoli_trening.csv")
        List<List<String>> irisTreningContent = csvReader("tren3.csv");

        //Utworzenie tablicy wartości dla obiektów treningowych
        double[][] treningAttributes = parseAtributtesToDoubleArray(irisTreningContent);

        //Utworzenie tablicy decyzji dla obiektów treningowych
        String[] treningDecisions = getDecisionFromObjects(irisTreningContent);


        //Wczytanie danych obiektów testowych (W CELU SPRAWDZENIA DLA ECOLI WYSTARCZY ZMIENIC NAZWE W PONIZSZYM CSV READER NA "ecoli_test.csv")
        List<List<String>> irisTestContent = csvReader("test3.csv");

        //Utworzenie tablicy wartości dla obiektów testowych
        double[][] testAttributes = parseAtributtesToDoubleArray(irisTestContent);

        //Utworzenie listy zawierajacej decyzje dla obiektow testowych
        ArrayList<ArrayList<String>> treningDecisionsForAllTestObjects = createDecisionForAllTestObject(treningDecisions, testAttributes.length);

        //Liczenie odleglosci pomiedzy punktami i zapisywanie do tablicy results
        double[][] results = countingDistanceOfObjectsForAllTestsObjects(treningAttributes, testAttributes);

        //Sortowanie odleglosci rownolegle z decyzjami obiektow testowych
        quickSortForAllObjects(results, treningDecisionsForAllTestObjects);

        //Stworzenie slownikow z "k" decyzjami i odleglosciami dla testowych obiektow
        ArrayList<HashMap<String, Integer>> dictionaries = createDictionariesForTestObjects(k, treningDecisionsForAllTestObjects);

        //Pobiera decyzje, ktora ma najwiecej zliczen, jesli jest sytuacja ze kilka decyzji ma takie same wartosci - wybiera losowo
        String[] decisions = getCorrectDecision(dictionaries);

        //Wyswietlanie wyniku na ekran
        printDecisions(decisions, k, dictionaries);

        //Informacja o czasie trwania programu
        System.out.println();
        System.out.println("Wczytanie plikow .csv oraz caly algorytm zajal programowi " + (Calendar.getInstance().getTimeInMillis() - timeMilli) + " ms");
    }

    //funkcja wyswietla decyzje wraz z zestawieniem najblizszych odleglosci
    public static void printDecisions(String[] decisions, int k, ArrayList<HashMap<String, Integer>> dictionaries) {

        System.out.println("Wyniki dla " + k + " sasiadow:");
        for (int i = 0; i < decisions.length; i++) {
            System.out.println();
            System.out.println("Dla " + (i + 1) + " obiektu testowego decyzją będzie " + decisions[i] + " | Zestawienie czestosci wystepowania klas najblizszych sasiadow: " + dictionaries.get(i));
        }

    }

    //funkcja zwaraca wartosc maksymalna zliczen decyzji, ewentualnie wybiera losowo jesli jest taka potrzeba
    public static String[] getCorrectDecision(ArrayList<HashMap<String, Integer>> dictionaries) {

        String[] decisions = new String[dictionaries.size()];

        for (int i = 0; i < dictionaries.size(); i++) {
            decisions[i] = dictionaries.get(i).entrySet().stream().max((entry1, entry2) -> entry1.getValue() >= entry2.getValue() ? entry1.getValue() == entry2.getValue() ? ThreadLocalRandom.current().nextInt(-1, 1) : 1 : -1).get().getKey();
        }
        return decisions;
    }

    //Funkcja tworzy slowniki dla obiektow testowych(Decyzja:Ilosc zliczen)
    public static ArrayList<HashMap<String, Integer>> createDictionariesForTestObjects(int k, ArrayList<
            ArrayList<String>> treningDecisionsForAllTestObjects) {

        ArrayList<HashMap<String, Integer>> dictionaries = new ArrayList<>();
        for (int i = 0; i < treningDecisionsForAllTestObjects.size(); i++) {
            dictionaries.add(new HashMap<>());
        }

        for (int i = 0; i < treningDecisionsForAllTestObjects.size(); i++) {
            for (int j = 0; j < k; j++) {
                if (dictionaries.get(i).containsKey(treningDecisionsForAllTestObjects.get(i).get(j))) {
                    int count = dictionaries.get(i).get(treningDecisionsForAllTestObjects.get(i).get(j));
                    dictionaries.get(i).put(treningDecisionsForAllTestObjects.get(i).get(j), count + 1);
                } else {
                    dictionaries.get(i).put(treningDecisionsForAllTestObjects.get(i).get(j), 1);
                }
            }

        }
        return dictionaries;
    }

    //Funkcja wywoluje funkcje algorytmu sortowania quickSort dla poszczegolnego obiektu testowego
    public static void quickSortForAllObjects(double[][] resultsBeforeSorting, ArrayList<ArrayList<String>>
            treningDecisionsForAllTestObjects) {

        for (int i = 0; i < resultsBeforeSorting.length; i++) {
            quickSortForOneObject(resultsBeforeSorting[i], 0, resultsBeforeSorting[i].length - 1, treningDecisionsForAllTestObjects.get(i));
        }

    }

    //W funkcji zaimplementowany zostal algorytm sortowania quickSort, sortuje odleglosci pomiedzy obiektami oraz rownolegle sortuje wartosci decyzji
    private static void quickSortForOneObject(double[] distanceOfOneTestObject, int low, int high, ArrayList<
            String> decisionOfOneTestObject) {

        int i = low, j = high;
        double pivot = distanceOfOneTestObject[low + (high - low) / 2];

        double exchangeAttributeValue;
        String exchangeDistanceObject;


        while (i <= j) {

            while (distanceOfOneTestObject[i] < pivot) {
                i++;
            }

            while (distanceOfOneTestObject[j] > pivot) {
                j--;
            }


            if (i <= j) {

                exchangeAttributeValue = distanceOfOneTestObject[i];
                exchangeDistanceObject = decisionOfOneTestObject.get(i);


                distanceOfOneTestObject[i] = distanceOfOneTestObject[j];

                decisionOfOneTestObject.set(i, decisionOfOneTestObject.get(j));


                distanceOfOneTestObject[j] = exchangeAttributeValue;
                decisionOfOneTestObject.set(j, exchangeDistanceObject);


                i++;
                j--;
            }
        }
        if (low < j)
            quickSortForOneObject(distanceOfOneTestObject, low, j, decisionOfOneTestObject);
        if (i < high)
            quickSortForOneObject(distanceOfOneTestObject, i, high, decisionOfOneTestObject);
    }


    // Funkcja odpowiada za wczytanie danych z pliku csv
    public static List<List<String>> csvReader(String filepath) {
        List<List<String>> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(COMMA_DELIMITER);
                records.add(Arrays.asList(values));
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return records;
    }

    // Funkcja pozwala na wybranie decyzji z obiektow wczytanych z pliku csv
    public static String[] getDecisionFromObjects(List<List<String>> list) {
        String[] dec = new String[list.size()];

        for (int i = 0; i < dec.length; i++) {
            dec[i] = list.get(i).get(list.get(i).size() - 1);
        }
        return dec;
    }

    //Funkcja tworzy liste decyzji dla obiektow testowych
    public static ArrayList<ArrayList<String>> createDecisionForAllTestObject(String[] treningDecisions,
                                                                              int numbersOfTestObjects) {
        ArrayList<ArrayList<String>> treningDecisionsForAllObjects = new ArrayList<>(treningDecisions.length - 1);
        for (int i = 0; i < numbersOfTestObjects; i++) {
            treningDecisionsForAllObjects.add(new ArrayList<String>());
            for (int j = 0; j < treningDecisions.length; j++) {
                treningDecisionsForAllObjects.get(i).add(treningDecisions[j]);
            }
        }
        return treningDecisionsForAllObjects;
    }

    //Funkcja wprowadza wartosci atrybutow do tablicy, wywolywana po wczytaniu pliku csv
    public static double[][] parseAtributtesToDoubleArray(List<List<String>> list) {

        boolean isDecOnLastArrayElement = true;
        double[][] attributes;

        try {
            Double.parseDouble(list.get(0).get(list.get(0).size() - 1));
            isDecOnLastArrayElement = false;
        } catch (Exception ex) {
        }

        if (isDecOnLastArrayElement == true) {
            attributes = new double[list.size()][list.get(0).size() - 1];
            for (int i = 0; i < list.size(); i++) {
                for (int j = 0; j < list.get(0).size() - 1; j++) {
                    attributes[i][j] = Double.parseDouble(list.get(i).get(j));
                }
            }
        } else {
            attributes = new double[list.size()][list.get(0).size()];
            for (int i = 0; i < list.size(); i++) {
                for (int j = 0; j < list.get(0).size(); j++) {
                    attributes[i][j] = Double.parseDouble(list.get(i).get(j));
                }
            }
        }

        return attributes;
    }

    //Funkcja zwraca wynik czastkowy liczenia odleglosci za pomoca metody Euklidesa
    private static double countDifferenceAndPowerOfOneObjectAttribute(double treningAttributes,
                                                                      double testAttributes) {
        return Math.pow(treningAttributes - testAttributes, 2);
    }

    //Funkcja laczy wszystkie wyniki czastkowe liczenia odleglosci za pomoca metody Euklidesa oraz wyciaga pierwiastek wedle wzoru
    private static double countingDistanceBetweenObject(double[] treningAttributesOfOneObject,
                                                        double[] testAttributesOfOneObject) {
        double resultOfOneObject = 0;


        for (int i = 0; i < treningAttributesOfOneObject.length; i++) {
            resultOfOneObject += countDifferenceAndPowerOfOneObjectAttribute(treningAttributesOfOneObject[i], testAttributesOfOneObject[i]);
        }
        resultOfOneObject = Math.sqrt(resultOfOneObject);

        return resultOfOneObject;
    }

    //Funkcja wywoluje funkcje do wyliczenia odlegosci dla pojedynczego obiektu testowego
    private static double[] countingDistanceOfObjectsForOneTestObject(double[][] treningAttributes,
                                                                      double[] testAttributes) {
        double[] resultOfOneTestObject = new double[treningAttributes.length];
        for (int i = 0; i < treningAttributes.length; i++) {
            resultOfOneTestObject[i] = countingDistanceBetweenObject(treningAttributes[i], testAttributes);

        }

        return resultOfOneTestObject;
    }

    //Funkcja, przy ktorej uzyciu wyliczamy wszystkie odleglosci dla obiektow testowych
    public static double[][] countingDistanceOfObjectsForAllTestsObjects(double[][] treningAttributes,
                                                                         double[][] testAttributes) {
        double[][] results = new double[testAttributes.length][treningAttributes.length];
        for (int i = 0; i < testAttributes.length; i++) {
            results[i] = countingDistanceOfObjectsForOneTestObject(treningAttributes, testAttributes[i]);
        }

        return results;
    }


}





