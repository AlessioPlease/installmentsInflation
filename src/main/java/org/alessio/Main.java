package org.alessio;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.util.Pair;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

// Scopo: attualizzare i prezzi di delle rate nel passato
public class Main {
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		int[] dateRange = getDateRange(scanner);
		String amount = getAmount(scanner);
		scanner.close();
		int startMonth = dateRange[0], startYear = dateRange[1], endMonth = dateRange[2], endYear = dateRange[3];
		ArrayList<Pair<String, String>> monthYearRange = getMonthYearRange(startMonth, startYear, endMonth, endYear);

		for (Pair<String, String> pair: monthYearRange) {
			//System.out.println(pair.getKey() + " " + pair.getValue());
			try {
				String inflationData = getInflationData(pair.getKey(), pair.getValue(), amount);
				JSONObject jsonObject = new JSONObject(inflationData);
				String coefficiente = jsonObject.getString("Coefficiente");
				String euro = jsonObject.getString("Euro");
				System.out.println(pair.getKey() + " " + pair.getValue() + "; " + coefficiente + "; " + euro);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method prompts the user to input a start date and an end date in the format MM.YYYY.
	 * It validates the input dates to ensure they are in the correct format, the month is between 01 and 12,
	 * and the year is between 1947 and the current year. It also ensures the end date is after the start date.
	 *
	 * @param scanner the Scanner object used to read user input
	 * @return an array of integers containing the start month, start year, end month, and end year
	 */
	public static int[] getDateRange(Scanner scanner) {
		String startDate = "", endDate = "";
		int startMonth = 0, startYear = 0, endMonth = 0, endYear = 0;
		Pattern pattern = Pattern.compile("^(\\d{2})\\.(\\d{4})$");
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		boolean areDatesValid = false;

		do {
			System.out.println("Inserisci la data di inizio (MM.YYYY): ");
			startDate = scanner.nextLine();
			if (!pattern.matcher(startDate).matches()) {
				System.out.println("Invalid date format. Please use MM.YYYY format.");
				continue;
			} else {
				Matcher startMatcher = pattern.matcher(startDate);
				startMatcher.find();
				startMonth = Integer.parseInt(startMatcher.group(1)) - 1; // Convert to 0-based index
				startYear = Integer.parseInt(startMatcher.group(2));
				if (startMonth < 0 || startMonth > 11) {
					System.out.println("Month must be between 01 and 12.");
					continue;
				}
				if (startYear < 1947 || startYear > currentYear) {
					System.out.println("Year must be between 1947 and the current year.");
					continue;
				}
			}
			System.out.println("Inserisci la data di fine (MM.YYYY): ");
			endDate = scanner.nextLine();
			if (!pattern.matcher(endDate).matches()) {
				System.out.println("Invalid date format. Please use MM.YYYY format.");
				continue;
			} else {
				Matcher endMatcher = pattern.matcher(endDate);
				endMatcher.find();
				endMonth = Integer.parseInt(endMatcher.group(1)) - 1; // Convert to 0-based index
				endYear = Integer.parseInt(endMatcher.group(2));
				if (endMonth < 0 || endMonth > 11) {
					System.out.println("Month must be between 01 and 12.");
					continue;
				}
				if (endYear < 1947 || endYear > currentYear) {
					System.out.println("Year must be between 1947 and the current year.");
					continue;
				}
				if (endYear < startYear || (endYear == startYear && endMonth < startMonth)) {
					System.out.println("End date must be after the start date.");
					continue;
				}
			}
			areDatesValid = true;

		} while (!areDatesValid);

		return new int[]{startMonth, startYear, endMonth, endYear};
	}

	/**
	 * Prompts the user to input an amount to be updated to the current value.
	 * The method ensures that the input is a valid positive integer.
	 *
	 * @param scanner the Scanner object used to read user input
	 * @return a string representing the valid amount entered by the user
	 */
	public static String getAmount(Scanner scanner) {
		boolean isAmountValid = false;
		String amount = "";

		do {
			System.out.println("Inserisci l'importo della data da attualizzare (solo numeri interi): ");
			amount = scanner.nextLine();
			try {
				int amountValue = Integer.parseInt(amount);
				if (amountValue > 0) {
					isAmountValid = true;
				} else {
					System.out.println("L'importo deve essere maggiore di 0.");
				}
			} catch (NumberFormatException e) {
				System.out.println("Formato non valido. Inserisci un numero intero.");
			}
		} while (!isAmountValid);

		return amount;
	}

	/**
	 * Fetches inflation data from the ISTAT website for a given month, year, and amount.
	 * The method sends a POST request to the ISTAT calculator and parses the response
	 * to extract the coefficient and euro values.
	 *
	 * @param month the month for which to fetch inflation data
	 * @param year the year for which to fetch inflation data
	 * @param amount the amount to be updated to the current value
	 * @return a JSON string containing the coefficient and euro values
	 * @throws IOException if an I/O error occurs
	 */
	public static String getInflationData(String month, String year, String amount) throws IOException {
		String url = "https://rivaluta.istat.it/Rivaluta/CalcolatoreCoefficientiAction.action";
		String urlParameters = "SELEZIONE=HOME&CARTELLA&PERIODO=1&meseDa=" + month + "&annoDa=" + year + "&meseA=Dicembre&annoA=2024&SOMMA=" + amount + "&EUROLIRE=true";

		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

		try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
			out.writeBytes(urlParameters);
			out.flush();
		}


		StringBuilder response;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String inputLine;
			response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
		}

		Document doc = Jsoup.parse(response.toString());
		Element coefficienteElement = doc.select("input[readonly][type=text]").get(0);
		Element euroElement = doc.select("input[readonly][type=text]").get(1);
		String coefficiente = coefficienteElement.attr("value");
		String euro = euroElement.attr("value");

		String htmlResponse = response.toString();
		String coefficienteRegex = htmlResponse.split("value=\"")[1].split("\"")[0];
		String euroRegex = htmlResponse.split("value=\"")[2].split("\"")[0];

		return String.format("{\"Coefficiente\":\"%s\",\"Euro\":\"%s\"}", coefficiente, euro);
	}

	/**
	 * Generates a list of month-year pairs between the specified start and end dates.
	 * The method iterates through each month and year in the range and adds a pair
	 * of month name and year to the result list.
	 *
	 * @param startMonth the starting month
	 * @param startYear the starting year
	 * @param endMonth the ending month
	 * @param endYear the ending year
	 * @return an ArrayList of Pair objects, each containing a month name and a year
	 */
	public static ArrayList<Pair<String, String>> getMonthYearRange(int startMonth, int startYear, int endMonth, int endYear) {
		String[] months = {"Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno", "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"};
		ArrayList<Pair<String, String>> result = new ArrayList<>();

		for (int year = startYear; year <= endYear; year++) {
			int monthStart = (year == startYear) ? startMonth : 0;
			int monthEnd = (year == endYear) ? endMonth : 11;
			for (int month = monthStart; month <=  monthEnd; month++) {
				result.add(new Pair<>(months[month], String.valueOf(year)));
			}
		}
		return result;
	}
}