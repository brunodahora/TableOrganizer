package table.organizer;

import java.util.ArrayList;
import java.util.List;

import table.organizer.exceptions.DuplicatePersonException;
import table.organizer.model.Consumable;
import table.organizer.model.Person;

public class TableManager {
	private int nextId;
	private List<Person> persons;
	private List<Consumable> consumables;
	private int tip;
	private static TableManager tableManager = new TableManager();

	public static TableManager getInstance(){
		return tableManager;
	}
	
	private TableManager() {
		persons = new ArrayList<Person>();
		consumables = new ArrayList<Consumable>();
		
		nextId = 0;
		tip = 10;
	}
	
	public int getTip() {
		return tip;
	}

	public void setTip(int tip) {
		this.tip = tip;
	}

	/**
	 * 
	 * @return total bill price in cents
	 */
	public int getTotalBill(){
		
		int price = 0;
		
		for(Consumable consumable : consumables){
			price += consumable.getTotalPrice();
		}
		
		return price;
	}
	
	Person addPerson(String name) throws DuplicatePersonException {

		for (Person person : persons) {
			if(person.getName().equals(name)){
				throw new DuplicatePersonException("Person already exists");
			}
		}
		
		Person newPerson = new Person(name);
		
		persons.add(newPerson);
		
		return newPerson;
	}
	
	boolean removePerson(String name){
		Person person = getPersonByName(name);
		if(person == null){
			return false;
		}
		for (Consumable consumable : person.getConsumables()) {
			consumable.removePerson(person);
		}
		return persons.remove(new Person(name));
	}
	
	private Person getPersonByName(String name) {
		for (Person person : persons) {
			if(person.getName().equals(name)){
				return person;
			}
		}
		return null;
	}

	Consumable addConsumable(String name, int price, int quantity){
		
		Consumable newConsumable = new Consumable(name, price, quantity, ++nextId);
		
		consumables.add(newConsumable);
		
		return newConsumable;
	}
	
	boolean removeConsumable(int id){
		Consumable consumable = getConsumableById(id);
		if (consumable == null)
			return false;
		for (Person person : consumable.getPersons()) {
			person.removeConsumable(consumable);
		}
		return consumables.remove(consumable);
	}
	
	private Consumable getConsumableById(int id) {
		for (Consumable consumable : consumables) {
			if (consumable.getId() == id){
				return consumable;
			}
		}
		return null;
	}

	void addConsumableToPerson(Consumable consumable, Person person){
		consumable.addPerson(person);
		person.addConsumable(consumable);
	}
	
	public int getNumberOfConsumables () {
		return consumables.size();
	}
	
	public Consumable getConsumable (int position) {
		return consumables.get(position);
	}
	
	public Person getPerson (int position) {
		return persons.get(position);
	}

	public int getNumberOfPersons() {
		return persons.size();
	}
}