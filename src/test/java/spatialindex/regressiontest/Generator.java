import java.util.*;
import java.io.*;

class MyRegion
{
	public double m_xmin;
	public double m_ymin;
	public double m_xmax;
	public double m_ymax;

	public MyRegion(double x1, double y1, double x2, double y2)
	{
		m_xmin = (x1 < x2) ? x1 : x2;
		m_ymin = (y1 < y2) ? y1 : y2;
		m_xmax = (x1 > x2) ? x1 : x2;
		m_ymax = (y1 > y2) ? y1 : y2;
	}

	public boolean intersects(MyRegion r)
	{
		if (m_xmin > r.m_xmax || m_xmax < r.m_xmin ||
				m_ymin > r.m_ymax || m_ymax < r.m_ymin) return false;

		return true;
	}

	public double getMinDist(final MyRegion r)
	{
		double ret = 0.0;

		if (r.m_xmin < m_xmin)
			ret += Math.pow(m_xmin - r.m_xmin, 2.0);
		else if (r.m_xmin > m_xmax)
				ret += Math.pow(r.m_xmin - m_xmax, 2.0);

		if (r.m_ymin < m_ymin)
				ret += Math.pow(m_ymin - r.m_ymin, 2.0);
		else if (r.m_ymin > m_ymax)
				ret += Math.pow(r.m_ymin - m_ymax, 2.0);

		return ret;
	}
}

public class Generator
{
	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.err.println("Usage: Generator number_of_data.");
			System.exit(-1);
		}

		Random rand = new Random();

		int numberOfObjects = new Integer(args[0]).intValue();
		HashMap data = new HashMap(numberOfObjects);

		for (int i = 0; i < numberOfObjects; i++)
		{
			MyRegion r = new MyRegion(rand.nextDouble(), rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
			data.put(new Integer(i), r);

			System.out.println("1 " + i + " " + r.m_xmin + " " + r.m_ymin + " " + r.m_xmax + " " + r.m_ymax);
		}

		int A = (int) (Math.floor((double) numberOfObjects * 0.1));

		for (int T = 100; T > 0; T--)
		{
			System.err.println(T);
			HashSet examined = new HashSet();

			for (int a = 0; a < A; a++)
			{
				// find an id that is not yet examined.
				Integer id = new Integer((int) ((double) numberOfObjects * rand.nextDouble()));
				boolean b = examined.contains(id);

				while (b)
				{
					id = new Integer((int) ((double) numberOfObjects * rand.nextDouble()));
					b = examined.contains(id);
				}
				examined.add(id);
				MyRegion r = (MyRegion) data.get(id);

				System.out.println("0 " + id + " " + r.m_xmin + " " + r.m_ymin + " " + r.m_xmax + " " + r.m_ymax);

				r = new MyRegion(rand.nextDouble(), rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
				data.put(id, r);

				System.out.println("1 " + id + " " + r.m_xmin + " " + r.m_ymin + " " + r.m_xmax + " " + r.m_ymax);
			}

			double stx = rand.nextDouble();
			double sty = rand.nextDouble();
			System.out.println("2 9999999 " + stx + " " + sty + " " + (stx + 0.01) + " " + (sty + 0.01));
		}
	}
}
