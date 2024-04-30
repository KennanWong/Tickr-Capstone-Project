import React from 'react'
import Box from '@mui/material/Box';
import Header from '../Components/Header'
import { Backdrop, BackdropNoBG, CentredBox, ContentBox } from '../Styles/HelperStyles';
import { styled } from '@mui/system';
import { Link } from "react-router-dom";
import Grid from '@mui/material/Grid';
import { Container, Divider } from '@mui/material';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import TabContext from '@mui/lab/TabContext';
import TabList from '@mui/lab/TabList';
import TabPanel from '@mui/lab/TabPanel';
import EventCard from '../Components/EventCard';
import Button from '@mui/material/Button';
import EventCardsBar from '../Components/EventCardsBar';
import { apiFetch, getToken, loggedIn } from '../Helpers';
import dayjs from 'dayjs';
import SwipeableViews from 'react-swipeable-views';
import ArrowBackIcon from "@mui/icons-material/ArrowBack"
import KeyboardArrowLeft from '@mui/icons-material/KeyboardArrowLeft';
import KeyboardArrowRight from '@mui/icons-material/KeyboardArrowRight';
import MobileStepper from '@mui/material/MobileStepper';
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

export default function UserHosting({}) {
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
          My Hosted Events
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
          <EventsBar endpoint={'/api/event/hosting'} additionalParams={{auth_token: getToken(), before: endOfWeek}} responseField={'eventIds'}/>
        </TabPanel>
        <TabPanel value="2" sx={{padding: 0}}>
          <EventsBar endpoint={'/api/event/hosting'} additionalParams={{auth_token: getToken(), before: endOfMonth}} responseField={'eventIds'}/>
        </TabPanel>
        <TabPanel value="3" sx={{padding: 0}}>
          <EventsBar endpoint={'/api/event/hosting'} additionalParams={{auth_token: getToken(), before: endOfYear}} responseField={'eventIds'}/>
        </TabPanel>
        <TabPanel value="4" sx={{padding: 0}}>
          <EventsBar endpoint={'/api/event/hosting/past'} additionalParams={{auth_token: getToken()}} responseField={'eventIds'}/>
        </TabPanel>
      </TabContext>
    </Section>
  )
}