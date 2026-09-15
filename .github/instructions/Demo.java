public class Main {
    public static void main(String[] args)
    {
        // fixed-size array of 5 integers
        int[] arr= new int[5]; 

        // storing values in the array
        for (int i = 0; i < arr.length; i++) {
            arr[i] = i + 1; // storing 1,2,3,4,5
        }

        // printing array elements
        System.out.print("Array elements are: ");
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i] + " ");
        }
    }
}