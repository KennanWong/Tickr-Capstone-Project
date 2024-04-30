import React from 'react'
import Box from '@mui/material/Box';
import Header from './Header'
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
import EventCard from './EventCard';
import Button from '@mui/material/Button';
import EventCardsBar from './EventCardsBar';
import { apiFetch, getToken } from '../Helpers';
import dayjs from 'dayjs';
import SwipeableViews from 'react-swipeable-views';
import ArrowBackIcon from "@mui/icons-material/ArrowBack"
import KeyboardArrowLeft from '@mui/icons-material/KeyboardArrowLeft';
import KeyboardArrowRight from '@mui/icons-material/KeyboardArrowRight';
import MobileStepper from '@mui/material/MobileStepper';
import { useTheme } from '@mui/material/styles';
import { useParams } from 'react-router-dom';
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

export default function HostEvents({userDetails}) {
  const theme = useTheme();
  const params = useParams()
  const [upcomingValue, setUpcomingValue] = React.useState('1');
  
  const upcomingChange = (event, newValue) => {
    setUpcomingValue(newValue)
  }; 
  
  return (
    <Section sx={{ml: 0, mr: 0}}>
      <TabContext value={upcomingValue}>
        <SectionHeading>
          <Box sx={{display: 'flex', alignItems: 'flex-end'}}>
            <Tabs
              onChange={upcomingChange}
              textColor="secondary"
              indicatorColor="secondary"
              scrollButtons
              value={upcomingValue}
              >
              <Tab label="Upcoming Events" value="1" />
              <Tab label="Past Events" value="2" />
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
        {/* Future Pannel */}
        <TabPanel value="1" sx={{padding: 0}}>
          <EventsBar endpoint={'/api/user/hosting/future'} responseField={'event_ids'} additionalParams={{email: userDetails.email}}/>
        </TabPanel>
        {/* Past Pannel */}
        <TabPanel value="2" sx={{padding: 0}}>
          <EventsBar endpoint={'/api/user/hosting/past'} responseField={'event_ids'} additionalParams={{email: userDetails.email}}/>
        </TabPanel>
      </TabContext>
    </Section>
  )
}