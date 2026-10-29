//Make sure to change a tiny bit with the prints but keep the out and in the same
//Remove the comments and replace with your own comments

public static void main(String[] args) {
        System.out.print("Connecting to request pipe... "); //change the output here
        out = new FileOutputStream(PIPE_REQ); // keep the same
        System.out.println("Done"); //could change

        System.out.print("Connecting to response pipe... "); //change
        in = new FileInputStream(PIPE_RES); //keep
        System.out.println("Done"); //could change
    }
