import React from 'react'
import Box from '@mui/material/Box';
import { styled } from '@mui/system';
import { Divider } from '@mui/material';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import TabContext from '@mui/lab/TabContext';
import TabPanel from '@mui/lab/TabPanel';
import EventCardsBar from '../Components/EventCardsBar';
import { apiFetch, getToken, loggedIn } from '../Helpers';
import dayjs from 'dayjs';
import { useTheme } from '@mui/material/styles';
import EventsBar from './EventsBar';

const Section = styled(Box)({
  marginLeft: '1.5%',
  marginRight: '1.5%',
  height: '500px',
  fontSize: '50px',
  fontWeight: 'bold',
})

const SectionHeading = styled(Box)({
  display: 'flex',
  justifyContent: 'flex-start',
  gap: '10px',
  marginBottom: '10px'
})

export default function UserBookings({}) {
  const theme = useTheme();
  const [upcomingValue, setUpcomingValue] = React.useState('1');
  
  const upcomingChange = (event, newValue) => {
    setUpcomingValue(newValue);
  }; 

  const endOfWeek = dayjs().endOf('week').toISOString()
  const endOfMonth = dayjs().endOf('month').toISOString()
  const endOfYear = dayjs().endOf('year').toISOString()

  return (
    <Section sx={{pt: 13}}>
      <TabContext value={upcomingValue}>
        <SectionHeading>
          My Bookings
          <Divider orientation="vertical" variant="middle" flexItem/>
          <Box sx={{display: 'flex', alignItems: 'flex-end'}}>
            <Tabs
              onChange={upcomingChange}
              textColor="secondary"
              indicatorColor="secondary"
              scrollButtons
              value={upcomingValue}
              >
              <Tab label="This Week" value="1" />
              <Tab label="This Month" value="2" />
              <Tab label="This Year" value="3" />
              <Tab label="Past" value="4" />
            </Tabs>
          </Box>
          <Box
            sx={{
              width: 'auto',
              display: 'flex',
              alignItems: 'flex-end',
              justifyContent: 'flex-end',
              paddingBottom: '6px',
              flexGrow: '4'
            }}
          >
            {/* <Button color='secondary'>
              see all
            </Button> */}
          </Box>
        </SectionHeading>
        <TabPanel value="1" sx={{padding: 0}}>
          <EventsBar endpoint={'/api/user/bookings'} additionalParams={{auth_token: getToken(), before: endOfWeek}} responseField={'event_id'}/>
        </TabPanel>
        <TabPanel value="2" sx={{padding: 0}}>
          <EventsBar endpoint={'/api/user/bookings'} additionalParams={{auth_token: getToken(), before: endOfMonth}} responseField={'event_id'}/>
        </TabPanel>
        <TabPanel value="3" sx={{padding: 0}}>
          <EventsBar endpoint={'/api/user/bookings'} additionalParams={{auth_token: getToken(), before: endOfYear}} responseField={'event_id'}/>
        </TabPanel>
        <TabPanel value="4" sx={{padding: 0}}>
          <EventsBar endpoint={'/api/user/bookings/past'} additionalParams={{auth_token: getToken()}} responseField={'event_id'}/>
        </TabPanel>
      </TabContext>
    </Section>
  )
}