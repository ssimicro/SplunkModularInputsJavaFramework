import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;


class test
{
    public test() {
    }

    public void run(int seed) {
	HashMap<Integer,Integer> randfound = new HashMap<Integer,Integer>();
	ArrayList<Integer> lotNum = new  ArrayList<Integer>();
	Random rand = new Random(seed);
	int pick=Math.abs(rand.nextInt()%49);
	if (pick==0)
	    pick++;
	int count=0;
	while (count<6) {
	    if (randfound.get(pick)==null) {
		lotNum.add(pick);
		randfound.put(pick, pick);
		count++;
	    }
	    else {
		pick=Math.abs(rand.nextInt()%49);
		if (pick==0)
		    pick++;
	    }
	}
	String testing="hello ";
	for(int i:lotNum) {
	    System.out.print(i + " ");
	    testing=testing+i+" ";
	}
	System.out.println();
	System.out.println(testing);
    }


    public static void main(String[] args) {
	test t=new test();
	int seed=50;
	while (true) {
	    t.run(seed);
	    seed++;
	}
    }
}