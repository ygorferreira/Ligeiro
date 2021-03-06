package br.ufrj.cos.pinel.ligeiro;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import br.ufrj.cos.pinel.ligeiro.common.Constants;
import br.ufrj.cos.pinel.ligeiro.common.Util;
import br.ufrj.cos.pinel.ligeiro.data.BaseClass;
import br.ufrj.cos.pinel.ligeiro.data.ClassUsage;
import br.ufrj.cos.pinel.ligeiro.data.Controller;
import br.ufrj.cos.pinel.ligeiro.data.Dependency;
import br.ufrj.cos.pinel.ligeiro.data.Entity;
import br.ufrj.cos.pinel.ligeiro.data.IBaseClass;
import br.ufrj.cos.pinel.ligeiro.data.Method;
import br.ufrj.cos.pinel.ligeiro.data.Service;
import br.ufrj.cos.pinel.ligeiro.exception.LigeiroException;

/**
 * @author Roque Pinel
 *
 */
public class ClassUsageReport
{
	private String fileName;

	private Collection<ClassUsage> classUsages = new ArrayList<ClassUsage>();

	/**
	 * @param fileName the report's file name
	 */
	public ClassUsageReport(String fileName)
	{
		this.fileName = fileName;
	}

	/**
	 * @param classUsages
	 */
	public ClassUsageReport(String fileName, Collection<ClassUsage> classUsages)
	{
		this(fileName);

		this.classUsages = classUsages;
	}

	/**
	 * @return the fileName
	 */
	public String getFileName()
	{
		return fileName;
	}

	/**
	 * Loads the ClassUsage from dependencies.
	 * 
	 * @param entities
	 * @param allClasses
	 * @param dependencyClasses
	 * @return the class usages
	 * @throws LigeiroException
	 */
	public Collection<ClassUsage> loadClassUsage(Map<String, Entity> entities, Map<String, IBaseClass> allClasses, Map<String, BaseClass> dependencyClasses) throws LigeiroException
	{
		for (BaseClass dependencyClass : dependencyClasses.values())
		{
			IBaseClass clazz1 = allClasses.get(dependencyClass.getName());
			if (clazz1 == null)
				continue;

			Set<String> methodSignatures = clazz1.getMethodsSignatures();

			Set<String> dependenciesAlreadyFound = new HashSet<String>();

			String type = "";

			if (clazz1 instanceof Controller)
				type = ClassUsage.USE_CASE;
			else if (clazz1 instanceof Service)
				type = ClassUsage.SERVICE;
			else if (clazz1 instanceof BaseClass)
				type = ClassUsage.CLASS;
			else
				throw new LigeiroException("Could not find class's type.");

			for (Method method : dependencyClass.getMethods())
			{
				// double check the method
				if (methodSignatures.contains(method.getName()))
				{
					if (!type.equals(ClassUsage.USE_CASE))
						dependenciesAlreadyFound = new HashSet<String>();

					for (Dependency dependency : method.getDependencies())
					{
						ClassUsage classUsage = new ClassUsage();

						classUsage.setType(type);

						// if it's a controller, use the use case's name
						if (type.equals(ClassUsage.USE_CASE))
							classUsage.setElement(((Controller) clazz1).getUseCase().getName());
						else
							classUsage.setElement(clazz1.getName());

						classUsage.setMethod(method.getName());

						IBaseClass clazz2 = allClasses.get(dependency.getValue());

						if (clazz2 != null && !dependenciesAlreadyFound.contains(clazz2.getName()))
						{
							// depends on a service
							if (clazz2 instanceof Service)
							{
								classUsage.setDependencyType(ClassUsage.SERVICE);
								classUsage.setDependency(clazz2.getName());

								classUsages.add(classUsage);
							}

							dependenciesAlreadyFound.add(clazz2.getName());
						}
						else
						{
							clazz2 = entities.get(dependency.getValue());

							// depends on a entity
							if (clazz2 != null && !dependenciesAlreadyFound.contains(clazz2.getName()))
							{
								classUsage.setDependencyType(ClassUsage.ENTITY);
								classUsage.setDependency(clazz2.getName());

								classUsages.add(classUsage);

								dependenciesAlreadyFound.add(clazz2.getName());
							}
						}
					}
				}
			}
		}

		return classUsages;
	}

	/**
	 * Load the class usages from the CSV.
	 * 
	 * @return the class usages
	 * @throws LigeiroException
	 */
	public Collection<ClassUsage> loadCSV() throws LigeiroException
	{
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(fileName));

			boolean isHeader = true;
			
			while (in.ready())
			{
				String[] columns = Util.getCSVColumns(in.readLine(), Constants.CSV_DELIMITER);

				ClassUsage classUsage = new ClassUsage(columns[0], columns[1], columns[2], columns[3], columns[4]);

				Util.println(classUsage.getType() + " - " + classUsage.getElement() + " - "
						+ classUsage.getMethod() + " - " + classUsage.getDependencyType() + " - "
						+ classUsage.getDependency());

				if (isHeader)
					isHeader = false;
				else
					classUsages.add(classUsage);
			}

			in.close();
		}
		catch (FileNotFoundException e)
		{
			throw new LigeiroException("Could not find file: " + fileName, e);
		}
		catch (IOException e)
		{
			throw new LigeiroException("Could not read file: " + fileName, e);
		}

		return classUsages;
	}

	/**
	 * Creates the CSV report.
	 * 
	 * @throws LigeiroException
	 */
	public void createCSV() throws LigeiroException
	{
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(fileName));

			out.write("Type" + Constants.CSV_DELIMITER + "Element" + Constants.CSV_DELIMITER
					+ "Element Method" + Constants.CSV_DELIMITER + "Dependency Type" + Constants.CSV_DELIMITER
					+ "Dependency" + "\n");
	
			for (ClassUsage classUsage : classUsages)
			{
				out.write(classUsage.getType() + Constants.CSV_DELIMITER);

				out.write(classUsage.getElement() + Constants.CSV_DELIMITER);

				out.write(classUsage.getMethod() + Constants.CSV_DELIMITER);

				out.write(classUsage.getDependencyType() + Constants.CSV_DELIMITER);

				out.write(classUsage.getDependency() + "\n");
			}

			out.close();
		}
		catch (IOException e)
		{
			throw new LigeiroException("Could not write file: " + fileName, e);
		}
	}
}
