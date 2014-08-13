library ieee;
use ieee.std_logic_1164.all;
use ieee.std_logic_unsigned.all;
library ieee_proposed;
use ieee_proposed.fixed_pkg.all;
use ieee_proposed.fixed_float_types.ALL;
use std.textio.all;
use ieee.std_logic_textio.all; -- if you're saving this type of signal
entity paramexp_tb is
end paramexp_tb;

architecture tb of paramexp_tb is

component ParamExp is
	port(
		clk		: In  Std_logic;
		rst		: In  Std_logic;
		Start	: In  Std_logic;
		Done	: Out  Std_logic;
		X		: In sfixed(11 downto -12);
		Output	: Out sfixed(11 downto -12)
		);
end component;
signal clk 			: std_logic := '0';
signal rst 			: std_logic := '0';
signal Start 			: std_logic := '0';
signal Done 			: std_logic := '0';
signal X 			: sfixed(11 downto -12);
signal Output 			: sfixed(11 downto -12);
begin
	
	ParamExp_uut : ParamExp 
    port map (	clk => clk,
				rst => rst,
				Start => Start,
				Done => Done,
				X => X,
				Output => Output
				);
		

	process
	begin
	wait for 10ns;
	clk <= not(clk);
	wait for 10ns;
	clk <= not(clk);
	end process;
	
	process (Done) 
	begin
		if Done'event and Done = '1' then
			report "The value of 'output' is " & to_string(Output);
		end if;
	end process;
		
	process 
	begin            
	   -- wait for Reset to complete
	   -- wait until rst='1';
	   rst<='1';
	   wait for 40 ns;
	   rst<='0';
	   wait for 40 ns;
		X <= to_sfixed(0.01,11,-12);
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 200 ns;
		X <= to_sfixed(0.1,11,-12);
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 200 ns;
		X <= to_sfixed(1,11,-12);
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 200 ns;
		X <= to_sfixed(10,11,-12);
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 200 ns;
		X <= to_sfixed(100,11,-12);
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 200 ns;
    end process;
	
end tb;
		
