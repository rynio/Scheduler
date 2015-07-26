package scheduler.web;

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import scheduler.models.Account;
import scheduler.models.Genetic;
import scheduler.models.Resource;
import scheduler.models.Schedule;
import scheduler.models.Slot;
import scheduler.repositories.AppRepo;
import scheduler.security.SecurityHelper;

@RestController
@RequestMapping(value = "/rest")
public class SchedulesController {

	@Autowired
	AppRepo scheduleService;

	@RequestMapping("/user")
	public Principal user(Principal user) {
		return user;
	}
	
	@RequestMapping("/account/{name}") 
	public Account account(@PathVariable String name){
		return scheduleService.findByAccountName(name);
	}
	
	@RequestMapping("/logout")
	public void logout(Principal user) {
		SecurityContextHolder.getContext().setAuthentication(null);
	}
	
    @RequestMapping( value="/{accountId}", method = RequestMethod.GET)
	public Account getAccount(@PathVariable Long accountId) {
	    return scheduleService.findAccount(accountId);
	}

	@RequestMapping(value = "/{accountId}/schedules", method = RequestMethod.GET)
	public @ResponseBody List<Schedule> schedules(@PathVariable Long accountId) {
            if(SecurityHelper.isUserLoggedIn(scheduleService, accountId)) {
            	return scheduleService.getSchedulesByAccountId(accountId);
            }
		return null;
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{accountId}/schedules", method = RequestMethod.POST)
	public void createSchedule(@PathVariable Long accountId, @RequestBody Schedule schedule) {		
		if(SecurityHelper.isUserLoggedIn(scheduleService, accountId)) {
				Account account = scheduleService.findAccount(accountId);
				schedule.setOwner(account);
        		schedule.setDays(5);
        		schedule.setHours(5);
        		schedule.setIterations(100);
        		schedule.setPopulationSize(10);
        		scheduleService.createSchedule(schedule);
		}
	}
	
	@RequestMapping(value = "/{accountId}/schedules/{id}", method = RequestMethod.DELETE)
	public void deleteSchedule(@PathVariable Integer id) {
		scheduleService.deleteSchedule(id);
	}

	@RequestMapping(value = "/{accountId}/schedules/{id}", method = RequestMethod.GET)
	public @ResponseBody Schedule getSchedule(@PathVariable Integer id) {
		return scheduleService.getSchedule(id);
	}

	public ArrayList<ArrayList<String>> prepareGrid(String name, List<Slot> slots, List<Resource> resources, int dAYS, int hOURS) {
		
		ArrayList<ArrayList<String>> grid = new ArrayList<ArrayList<String>>();
			for (int i = 0; i < hOURS; i++) {
				ArrayList<String> row = new ArrayList<String>();
				for (int j = 0; j < dAYS; j++) {
					row.add(new String());
				}
				grid.add(row);
			}
			List<Slot> data = getSlotsBy(name, slots);
			for (Slot slot : data) {
				grid.get(slot.hour).set(
						slot.day,
						slot.subject + "\n" + slot.students + "\n"
								+ slot.teacher + "\n" + slot.room);
			}
			return grid;
	}

	public List<Slot> getSlotsBy(String name, List<Slot> slots)  {
		List<Slot> list = new ArrayList<Slot>();
		for (Slot slot : slots) {
			if (slot.students.equals(name) || slot.teacher.equals(name) || slot.room.equals(name)) {
				list.add(slot);
			}
		}
		return list;
	}

	@RequestMapping(value = "/{accountId}/schedules/{id}/resources", method = RequestMethod.GET)
	public @ResponseBody List<Resource> getResources(@PathVariable Integer id) {
		Schedule schedule = scheduleService.getSchedule(id);
		return schedule.getResources();
	}

	@RequestMapping(value = "/{accountId}/schedules/{id}/resources/{resid}", method = RequestMethod.GET)
	public @ResponseBody ArrayList<ArrayList<String>> grids(@PathVariable Integer resid, @PathVariable Integer id) {
		Schedule schedule = scheduleService.getSchedule(id);
		Resource res = scheduleService.getResource(resid);
		return prepareGrid(res.getName(), schedule.getSlots(), schedule.getResources(), schedule.getDays(), schedule.getHours());
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{accountId}/schedules/{id}/slots", method = RequestMethod.POST)
	public @ResponseBody void createSlot(@PathVariable Integer id, @RequestBody Slot data) {
		scheduleService.createSlot(id, data);
		for(int i = 0; i < data.duration-1; i++){
			scheduleService.createSlotCopy(data.getId());
		}
	}
	
	@RequestMapping(value = "/{accountId}/schedules/{id}/slots/{slotId}", method = RequestMethod.DELETE)
	public @ResponseBody void deleteSlot(@PathVariable Integer slotId) {
		scheduleService.deleteSlot(slotId);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{accountId}/schedules/{id}/resources", method = RequestMethod.POST)
	public @ResponseBody void createResource(@PathVariable Integer id, @RequestBody Resource data) {
		scheduleService.createResource(id, data);
	}
	
	@RequestMapping(value = "/{accountId}/schedules/{id}/slots", method = RequestMethod.GET)
	public @ResponseBody List<Slot> getSlots(@PathVariable Integer id) {
		Schedule schedule = scheduleService.getSchedule(id);
		return schedule.getSlots();
	}

	@RequestMapping(value = "/{accountId}/schedules/{id}/rates", method = RequestMethod.GET)
	public @ResponseBody List<Double> getRates(@PathVariable Integer id) {
		return scheduleService.getSchedule(id).getRates();
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{accountId}/schedules/{id}/generate", method = RequestMethod.POST)
	public @ResponseBody void generate(@PathVariable Integer id, @RequestBody Genetic params) throws CloneNotSupportedException {
		Schedule schedule = scheduleService.getSchedule(id);
		
		Genetic genetic = new Genetic(schedule);
		genetic.setDays(params.getDays());
		genetic.setHours(params.getHours());
		genetic.setiterations(params.getIterations());
		genetic.setPopulationSize(params.getPopulationSize());
		
		schedule = genetic.optimize();
		schedule.setDays(params.getDays());
		schedule.setHours(params.getHours());
		schedule.setIterations(params.getIterations());
		schedule.setPopulationSize(params.getPopulationSize());
		schedule.setRate(genetic.getRate());
		schedule.setRates(genetic.rates);
		
		scheduleService.updateSchedule(schedule);
	}
}